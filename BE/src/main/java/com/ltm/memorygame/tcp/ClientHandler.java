package com.ltm.memorygame.tcp;

import com.ltm.memorygame.service.user.UserService;
import com.ltm.memorygame.service.room.RoomService;
import com.ltm.memorygame.service.notification.NotificationService;
import com.ltm.memorygame.security.JwtService;
import com.ltm.memorygame.model.enums.UserStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class ClientHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);
    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Map<String, ClientHandler> onlineClients;

    private final UserService userService;
    private final RoomService roomService;
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
                         NotificationService notificationService,
                         JwtService jwtService,
                         boolean requireJwt,
                         int maxPerSecond) throws IOException {
        this.socket = socket;
        this.onlineClients = onlineClients;
        this.userService = userService;
        this.roomService = roomService;
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
                TCPMessage msg = JsonUtil.fromJson(raw, TCPMessage.class);
                handleMessage(msg);
            }
        } catch (IOException e) {
            log.warn("[TCP] Client disconnected: {}", username);
        } finally {
            if (username != null) {
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
                
                // Exit all rooms when disconnecting
                if (userId != null) {
                    try {
                        var rooms = roomService.findRoomsByPlayer(userId);
                        for (var room : rooms) {
                            try {
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
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }


    private void handleMessage(TCPMessage message) {
        switch (message.getType()) {
            case "LOGIN_REQUEST" -> handleLogin(message);
            case "LOGOUT_REQUEST" -> handleLogout();
            case "WORLD_CHAT" -> handleWorldChat(message);
            case "PRIVATE_CHAT" -> handlePrivateChat(message);
            case "ROOM_SETTINGS_CHANGED" -> handleRoomSettingsChanged(message);
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

    private void broadcastUserStatus(String user, boolean online) {
        TCPMessage msg = new TCPMessage("USER_STATUS",
                Map.of("user", user, "online", online), "server", null);
        for (ClientHandler client : onlineClients.values()) {
            client.sendMessage(msg);
        }
    }

    public void sendMessage(TCPMessage message) {
        synchronized (out) {
            out.println(JsonUtil.toJson(message));
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
