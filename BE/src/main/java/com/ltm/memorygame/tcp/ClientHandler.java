package com.ltm.memorygame.tcp;

import com.ltm.memorygame.service.user.UserService;
import com.ltm.memorygame.service.room.RoomService;
import com.ltm.memorygame.service.game.GameSessionService;
import com.ltm.memorygame.security.JwtService;
import com.ltm.memorygame.model.enums.UserStatus;
import com.ltm.memorygame.model.enums.RoomStatus;
import com.ltm.memorygame.model.game.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

public class ClientHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Map<String, ClientHandler> onlineClients;

    private final UserService userService;
    private final RoomService roomService;
    private final GameSessionService gameSessionService;
    private final JwtService jwtService;
    private final boolean requireJwt;
    private final int maxPerSecond;

    private long windowStartMs = System.currentTimeMillis();
    private int messagesInWindow = 0;

    private String username;
    private Long userId;

    public ClientHandler(Socket socket,
                         Map<String, ClientHandler> onlineClients,
                         UserService userService,
                         RoomService roomService,
                         GameSessionService gameSessionService,
                         JwtService jwtService,
                         boolean requireJwt,
                         int maxPerSecond) throws IOException {
        this.socket = socket;
        this.onlineClients = onlineClients;
        this.userService = userService;
        this.roomService = roomService;
        this.gameSessionService = gameSessionService;
        this.jwtService = jwtService;
        this.requireJwt = requireJwt;
        this.maxPerSecond = maxPerSecond;
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.out = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {
            String raw;
            while ((raw = in.readLine()) != null) {
                if (!allowMessage()) {
                    sendMessage(new TCPMessage("ERROR", Map.of("reason", "Rate limit exceeded"), "server", username));
                    continue;
                }
                log.debug("[TCP] Raw message received: {}", raw);
                try {
                    TCPMessage msg = JsonUtil.fromJson(raw, TCPMessage.class);
                    if (msg == null) {
                        log.warn("[TCP] Failed to parse message: null");
                        continue;
                    }
                    if (msg.getType() == null) {
                        log.warn("[TCP] Message has null type. Raw: {}", raw);
                        continue;
                    }
                    handleMessage(msg);
                } catch (Exception e) {
                    log.error("[TCP] Error parsing/handling message: {}", e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            log.warn("[TCP] Client disconnected: {}", username);
        } finally {
            if (username != null) {
                // Exit all rooms when disconnecting (BEFORE removing from onlineClients)
                // This ensures GAME_END messages can be broadcast to remaining players
                if (userId != null) {
                    try {
                        List<Room> rooms = roomService.findRoomsByPlayer(userId);
                        for (Room room : rooms) {
                            try {
                                // If room is playing, handle as player exit from game
                                if (room.getStatus() == RoomStatus.PLAYING) {
                                    log.info("[TCP] User {} exited during game in room {}", username, room.getId());
                                    try {
                                        // This will broadcast GAME_END to all online clients (including opponent)
                                        gameSessionService.handlePlayerExit(room.getId(), userId, username);
                                    } catch (Exception e) {
                                        log.warn("[TCP] Failed to handle game exit for user {}: {}", username, e.getMessage());
                                    }
                                }
                                
                                roomService.exitRoom(room.getId(), userId);
                                log.info("[TCP] User {} exited room {} on disconnect", username, room.getId());
                            } catch (Exception e) {
                                log.warn("[TCP] Failed to exit room {} for user {}: {}", 
                                    room.getId(), username, e.getMessage());
                            }
                        }
                    } catch (Exception e) {
                        log.warn("[TCP] Error during room cleanup for {}: {}", username, e.getMessage());
                    }
                }
                
                // Now remove from onlineClients AFTER handling game exit
                onlineClients.remove(username);
                
                // Update user status in database
                if (userId != null) {
                    try {
                        userService.setStatus(userId, UserStatus.OFFLINE);
                        log.info("[TCP] Set user {} status to OFFLINE", username);
                    } catch (Exception e) {
                        log.warn("[TCP] Failed to set status for user {}: {}", username, e.getMessage());
                    }
                }
                
                broadcastUserStatus(username, false);
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }


    private void handleMessage(TCPMessage message) {
        String msgType = message.getType();
        log.info("[TCP] Received message type: '{}' from user: {}", msgType, username);
        log.info("[TCP] Message type length: {}, equals COIN_FLIP_REQUEST: {}", 
                 msgType != null ? msgType.length() : "null", 
                 "COIN_FLIP_REQUEST".equals(msgType));
        switch (msgType) {
            case "LOGIN_REQUEST" -> handleLogin(message);
            case "LOGOUT_REQUEST" -> handleLogout();
            case "WORLD_CHAT" -> handleWorldChat(message);
            case "PRIVATE_CHAT" -> handlePrivateChat(message);
            case "ROOM_SETTINGS_CHANGED" -> handleRoomSettingsChanged(message);
            case "GAME_STARTED" -> handleGameStarted(message);
            case "CARD_FLIPPED" -> handleCardFlipped(message);
            case "CARD_MATCHED" -> handleCardMatched(message);
            case "TURN_SWITCH" -> handleTurnSwitch(message);
            case "CARDS_FLIP_BACK" -> handleCardsFlipBack(message);
            case "CARDS_FOR_MATCH_CHECK" -> handleCardsForMatchCheck(message);
            case "GAME_STATE_SYNC" -> handleGameStateSync(message);
            case "PLAYER_SURRENDER" -> handlePlayerSurrender(message);
            case "COIN_FLIP_REQUEST" -> handleCoinFlipRequest(message);
            case "PING" -> sendMessage(new TCPMessage("PONG", null, "server", username));
            default -> log.warn("[TCP] Unknown type: {}", message.getType());
        }
    }

    private void handleLogin(TCPMessage message) {
        Object nameObj = message.getData().get("username");
        Object tokenObj = message.getData().get("token");
        if (nameObj == null) {
            sendMessage(new TCPMessage("LOGIN_FAILED",
                    Map.of("reason", "Missing username"), "server", null));
            return;
        }
        this.username = nameObj.toString();
        onlineClients.put(username, this);

        if (!userService.existsByUsername(username)) {
            sendMessage(new TCPMessage("LOGIN_FAILED",
                    Map.of("reason", "Username not found in DB"), "server", null));
            onlineClients.remove(username);
            return;
        }

        if (requireJwt) {
            if (tokenObj == null) {
                sendMessage(new TCPMessage("LOGIN_FAILED",
                        Map.of("reason", "Missing token"), "server", null));
                onlineClients.remove(username);
                return;
            }
            String token = tokenObj.toString();
            if (!jwtService.validateToken(token)) {
                sendMessage(new TCPMessage("LOGIN_FAILED",
                        Map.of("reason", "Invalid token"), "server", null));
                onlineClients.remove(username);
                return;
            }
            String tokenUser = jwtService.extractUsername(token);
            if (tokenUser == null || !tokenUser.equals(username)) {
                sendMessage(new TCPMessage("LOGIN_FAILED",
                        Map.of("reason", "Token/user mismatch"), "server", null));
                onlineClients.remove(username);
                return;
            }
        }

        // Store userId for cleanup on disconnect
        try {
            this.userId = userService.getUserByUsername(username).getId();
        } catch (Exception e) {
            log.warn("[TCP] Could not get userId for {}: {}", username, e.getMessage());
        }
        
        // Set user status to ONLINE in database
        if (userId != null) {
            try {
                userService.setStatus(userId, UserStatus.ONLINE);
                log.info("[TCP] Set user {} status to ONLINE", username);
            } catch (Exception e) {
                log.warn("[TCP] Failed to set status for user {}: {}", username, e.getMessage());
            }
        }
        
        log.info("[TCP] User logged in: {}", username);
        sendMessage(new TCPMessage("LOGIN_SUCCESS", null, "server", username));
        broadcastUserStatus(username, true);
    }

    private void handleLogout() {
        if (username == null) return;
        onlineClients.remove(username);
        
        // Set user status to OFFLINE in database
        if (userId != null) {
            try {
                userService.setStatus(userId, UserStatus.OFFLINE);
                log.info("[TCP] Set user {} status to OFFLINE (logout)", username);
            } catch (Exception e) {
                log.warn("[TCP] Failed to set status for user {}: {}", username, e.getMessage());
            }
        }
        
        broadcastUserStatus(username, false);
        try { socket.close(); } catch (IOException ignored) {}
    }

    private void handleWorldChat(TCPMessage message) {
        for (ClientHandler client : onlineClients.values()) {
            client.sendMessage(message);
        }
    }

    private void handlePrivateChat(TCPMessage message) {
        String to = message.getReceiver();
        ClientHandler target = onlineClients.get(to);
        if (target != null) {
            target.sendMessage(message);
        } else {
            sendMessage(new TCPMessage("ERROR",
                    Map.of("reason", "User offline"), "server", username));
        }
    }

    private void handleRoomSettingsChanged(TCPMessage message) {
        log.info("[TCP] Room settings changed by {}: {}", username, message.getData());
        
        // Forward the message to all other clients in the same room
        // For now, we'll broadcast to all clients (can be optimized later)
        for (ClientHandler client : onlineClients.values()) {
            if (!client.username.equals(username)) { // Don't send back to sender
                client.sendMessage(message);
            }
        }
    }
    
    private void handleGameStarted(TCPMessage message) {
        log.info("[TCP] Game started by {}: {}", username, message.getData());

        // Mark the room as PLAYING so surrender/exit can be handled correctly
        try {
            if (userId != null) {
                java.util.List<com.ltm.memorygame.model.game.Room> myRooms = roomService.findRoomsByPlayer(userId);
                for (com.ltm.memorygame.model.game.Room room : myRooms) {
                    // Pick the READY room where I am the host and there is a guest
                    if (room.getStatus() == RoomStatus.READY
                            && room.getHost() != null
                            && username != null
                            && username.equals(room.getHost().getUsername())
                            && room.getGuest() != null) {
                        room.setStatus(RoomStatus.PLAYING);
                        roomService.updateAndMap(room);
                        log.info("[TCP] Room {} set to PLAYING", room.getId());
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[TCP] Failed to set room to PLAYING on GAME_STARTED: {}", e.getMessage());
        }

        // Forward the message to all other clients in the same room (temporary broadcast)
        for (ClientHandler client : onlineClients.values()) {
            if (!client.username.equals(username)) { // Don't send back to sender
                client.sendMessage(message);
            }
        }
    }

    private void broadcastUserStatus(String user, boolean online) {
        TCPMessage msg = new TCPMessage("USER_STATUS",
                Map.of("user", user, "online", online), "server", null);
        for (ClientHandler client : onlineClients.values()) {
            client.sendMessage(msg);
        }
    }

    private void handleCardFlipped(TCPMessage message) {
        log.info("[TCP] Card flipped by {}: {}", username, message.getData());
        // Forward the message to all other clients in the same room
        for (ClientHandler client : onlineClients.values()) {
            if (!client.username.equals(username)) { // Don't send back to sender
                client.sendMessage(message);
            }
        }
    }

    private void handleCardMatched(TCPMessage message) {
        log.info("[TCP] Card matched by {}: {}", username, message.getData());
        // Forward the message to all other clients in the same room
        for (ClientHandler client : onlineClients.values()) {
            if (!client.username.equals(username)) { // Don't send back to sender
                client.sendMessage(message);
            }
        }
    }

    private void handleTurnSwitch(TCPMessage message) {
        log.info("[TCP] Turn switch by {}: {}", username, message.getData());
        // Forward the message to all other clients in the same room
        for (ClientHandler client : onlineClients.values()) {
            if (!client.username.equals(username)) { // Don't send back to sender
                client.sendMessage(message);
            }
        }
    }
    
    private void handleCardsForMatchCheck(TCPMessage message) {
        log.info("[TCP] Cards for match check by {}: {}", username, message.getData());
        
        Map<String, Object> data = message.getData();
        if (data == null) {
            log.warn("[TCP] CARDS_FOR_MATCH_CHECK with no data");
            return;
        }
        
        Object roomIdObj = data.get("roomId");
        Object cardIndex1Obj = data.get("cardIndex1");
        Object cardIndex2Obj = data.get("cardIndex2");
        Object isHostObj = data.get("isHost");
        
        Long roomId = toLong(roomIdObj);
        Integer cardIndex1 = toInt(cardIndex1Obj);
        Integer cardIndex2 = toInt(cardIndex2Obj);
        Boolean isHost = (isHostObj != null) ? Boolean.valueOf(isHostObj.toString()) : Boolean.FALSE;
        
        if (roomId == null || cardIndex1 == null || cardIndex2 == null) {
            log.error("[TCP] Invalid CARDS_FOR_MATCH_CHECK payload: roomId={}, cardIndex1={}, cardIndex2={}", roomIdObj, cardIndex1Obj, cardIndex2Obj);
            return;
        }
        
        try {
            gameSessionService.processCardsForMatch(roomId, cardIndex1, cardIndex2, isHost, username);
        } catch (Exception e) {
            log.error("[TCP] Error processing cards for match check: {}", e.getMessage());
        }
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return (int) Math.round(Double.parseDouble(value.toString()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.longValue();
        try {
            return (long) Math.round(Double.parseDouble(value.toString()));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
    
    private void handleCardsFlipBack(TCPMessage message) {
        log.info("[TCP] Cards flip back by {}: {}", username, message.getData());
        // Forward the message to all other clients in the same room
        for (ClientHandler client : onlineClients.values()) {
            if (!client.username.equals(username)) { // Don't send back to sender
                client.sendMessage(message);
            }
        }
    }
    
    private void handleGameStateSync(TCPMessage message) {
        log.info("[TCP] Game state sync by {}: {}", username, message.getData());
        // Forward the message to all other clients in the same room
        for (ClientHandler client : onlineClients.values()) {
            if (!client.username.equals(username)) { // Don't send back to sender
                client.sendMessage(message);
            }
        }
    }
    
    private void handlePlayerSurrender(TCPMessage message) {
        log.info("[TCP] Player surrender by {}: {}", username, message.getData());

        if (userId == null) {
            log.warn("[TCP] Cannot handle surrender: userId is null for {}", username);
            return;
        }

        try {
            // Prefer roomId from message payload
            Long roomIdFromMsg = null;
            if (message.getData() != null && message.getData().get("roomId") != null) {
                try {
                    roomIdFromMsg = (long) Math.round(Double.parseDouble(message.getData().get("roomId").toString()));
                } catch (NumberFormatException ignored) {}
            }

            if (roomIdFromMsg != null) {
                Room room = roomService.getEntityById(roomIdFromMsg);
                if (room.getStatus() == RoomStatus.PLAYING) {
                    log.info("[TCP] {} surrendered in room {} (from payload)", username, roomIdFromMsg);
                    try {
                        gameSessionService.handlePlayerExit(roomIdFromMsg, userId, username);
                    } catch (Exception e) {
                        log.error("[TCP] Failed to handle surrender for {} in room {}: {}", username, roomIdFromMsg, e.getMessage());
                        e.printStackTrace();
                    }
                    // Update room occupancy/status after surrender
                    try {
                        roomService.exitRoom(roomIdFromMsg, userId);
                    } catch (Exception ex) {
                        log.warn("[TCP] Failed to update room {} after surrender: {}", roomIdFromMsg, ex.getMessage());
                    }
                    return;
                }
            }

            // Fallback: find playing room by membership
            List<Room> rooms = roomService.findRoomsByPlayer(userId);
            for (Room room : rooms) {
                if (room.getStatus() == RoomStatus.PLAYING) {
                    log.info("[TCP] {} surrendered from game in room {} (fallback)", username, room.getId());
                    try {
                        gameSessionService.handlePlayerExit(room.getId(), userId, username);
                    } catch (Exception e) {
                        log.error("[TCP] Failed to handle surrender for {}: {}", username, e.getMessage());
                        e.printStackTrace();
                    }
                    try {
                        roomService.exitRoom(room.getId(), userId);
                    } catch (Exception ex) {
                        log.warn("[TCP] Failed to update room {} after surrender: {}", room.getId(), ex.getMessage());
                    }
                    break; // Only handle one playing room
                }
            }
        } catch (Exception e) {
            log.error("[TCP] Error handling player surrender: {}", e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleCoinFlipRequest(TCPMessage message) {
        log.info("[TCP] ===== COIN_FLIP_REQUEST received from {} =====", username);
        log.info("[TCP] Coin flip request from {}: {}", username, message.getData());
        
        if (message.getData() == null) {
            log.warn("[TCP] COIN_FLIP_REQUEST has null data");
            return;
        }
        
        Object roomIdObj = message.getData().get("roomId");
        Long roomId = toLong(roomIdObj);
        
        if (roomId == null) {
            log.error("[TCP] COIN_FLIP_REQUEST missing roomId");
            return;
        }
        
        try {
            // Get room to find host and guest
            Room room = roomService.getEntityById(roomId);
            if (room == null) {
                log.error("[TCP] Room {} not found for coin flip", roomId);
                return;
            }
            
            if (room.getHost() == null) {
                log.error("[TCP] Room {} has no host", roomId);
                return;
            }
            
            if (room.getGuest() == null) {
                log.error("[TCP] Room {} has no guest", roomId);
                return;
            }
            
            String hostUsername = room.getHost().getUsername();
            String guestUsername = room.getGuest().getUsername();
            
            // Only host can request coin flip
            if (!username.equals(hostUsername)) {
                log.warn("[TCP] Non-host user {} tried to request coin flip for room {}", username, roomId);
                return;
            }
            
            // Random coin result: 1 or 2
            // 1 = host goes first, 2 = guest goes first
            // Use SecureRandom without seeding for true randomness
            java.security.SecureRandom secureRandom = new java.security.SecureRandom();
            
            // Test: generate multiple random values to verify randomness
            int test1 = secureRandom.nextInt(2) + 1;
            int test2 = secureRandom.nextInt(2) + 1;
            int test3 = secureRandom.nextInt(2) + 1;
            
            int coinResult = secureRandom.nextInt(2) + 1; // Returns 1 or 2
            
            log.info("[TCP] Coin flip for room {} - Test random values: [{}, {}, {}], Final Result: {} (1=host first, 2=guest first)", 
                    roomId, test1, test2, test3, coinResult);
            
            // Send result to both host and guest
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("coinResult", coinResult);
            resultData.put("roomId", roomId);
            
            TCPMessage resultMessage = new TCPMessage("COIN_FLIP_RESULT", resultData, "server", null);
            
            // Send to host
            ClientHandler hostHandler = onlineClients.get(hostUsername);
            if (hostHandler != null) {
                hostHandler.sendMessage(resultMessage);
                log.info("[TCP] Sent COIN_FLIP_RESULT to host: {}", hostUsername);
            } else {
                log.warn("[TCP] Host {} not online", hostUsername);
            }
            
            // Send to guest
            ClientHandler guestHandler = onlineClients.get(guestUsername);
            if (guestHandler != null) {
                guestHandler.sendMessage(resultMessage);
                log.info("[TCP] Sent COIN_FLIP_RESULT to guest: {}", guestUsername);
            } else {
                log.warn("[TCP] Guest {} not online", guestUsername);
            }
            
        } catch (Exception e) {
            log.error("[TCP] Error handling coin flip request: {}", e.getMessage());
            e.printStackTrace();
        }
    }

    public void sendMessage(TCPMessage message) {
        try {
            synchronized (out) {
                if (out != null && !out.checkError()) {
                    String json = JsonUtil.toJson(message);
                    out.println(json);
                    if (out.checkError()) {
                        log.warn("[TCP] Error writing message to client {}: {}", username, message.getType());
                    }
                } else {
                    log.warn("[TCP] Cannot send message to {}: output stream error or closed", username);
                }
            }
        } catch (Exception e) {
            log.error("[TCP] Exception sending message to {}: {}", username, e.getMessage());
        }
    }

    private boolean allowMessage() {
        long now = System.currentTimeMillis();
        if (now - windowStartMs >= 1000) {
            windowStartMs = now;
            messagesInWindow = 0;
        }
        messagesInWindow++;
        return messagesInWindow <= maxPerSecond;
    }

    public void shutdown() {
        try { socket.close(); } catch (IOException ignored) {}
    }
}
