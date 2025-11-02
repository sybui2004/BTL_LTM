package com.ltm.memorygame.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ltm.memorygame.dto.chat.request.MatchMessageRequest;
import com.ltm.memorygame.dto.chat.request.PrivateMessageRequest;
import com.ltm.memorygame.dto.chat.request.WorldMessageRequest;
import com.ltm.memorygame.dto.chat.response.MatchMessageDTO;
import com.ltm.memorygame.dto.chat.response.PrivateMessageResponse;
import com.ltm.memorygame.dto.chat.response.WorldMessageResponse;
import com.ltm.memorygame.dto.user.response.UserPresenceDTO;
import com.ltm.memorygame.model.enums.MessageType;
import com.ltm.memorygame.model.enums.UserStatus;
import com.ltm.memorygame.model.game.Room;
import com.ltm.memorygame.security.JwtService;
import com.ltm.memorygame.service.chat.MatchMessageService;
import com.ltm.memorygame.service.chat.PrivateMessageService;
import com.ltm.memorygame.service.chat.WorldMessageService;
import com.ltm.memorygame.service.notification.NotificationService;
import com.ltm.memorygame.service.room.RoomService;
import com.ltm.memorygame.service.user.PresenceService;
import com.ltm.memorygame.service.user.UserService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

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

    private final MatchMessageService matchMessageService;
    private final PrivateMessageService privateMessageService;
    private final WorldMessageService worldMessageService;
    private final PresenceService presenceService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

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
            int maxPerSecond,
            MatchMessageService matchMessageService,
            PrivateMessageService privateMessageService,
            WorldMessageService worldMessageService,
            PresenceService presenceService) throws IOException {
        this.socket = socket;
        this.onlineClients = onlineClients;
        this.userService = userService;
        this.roomService = roomService;
        this.jwtService = jwtService;
        this.requireJwt = requireJwt;
        this.maxPerSecond = maxPerSecond;
        this.matchMessageService = matchMessageService;
        this.privateMessageService = privateMessageService;
        this.worldMessageService = worldMessageService;
        this.presenceService = presenceService;
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
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handleMessage(TCPMessage message) {
        switch (message.getType()) {
            case "LOGIN_REQUEST" ->
                handleLogin(message);
            case "LOGOUT_REQUEST" ->
                handleLogout();
            case "WORLD_CHAT" ->
                handleWorldChat(message);
            case "PRIVATE_CHAT" ->
                handlePrivateChat(message);
            case "MATCH_CHAT" ->
                handleMatchChat(message);
            case "LOBBY_CHAT" ->
                handleLobbyChat(message);
            case "SET_STATUS_REQUEST" ->
                handleSetStatus(message);
            case "PING" ->
                sendMessage(new TCPMessage("PONG", null, "server", username));
            default ->
                log.warn("[TCP] Unknown type: {}", message.getType());
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
        if (username == null) {
            return;
        }
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
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private void handleWorldChat(TCPMessage message) {
        if (username == null || userId == null) {
            return;
        }

        WorldMessageRequest request;

        try {
            request = objectMapper.convertValue(message.getData(), WorldMessageRequest.class);

            // Validation 
            Set<ConstraintViolation<WorldMessageRequest>> violations = validator.validate(request);

            if (!violations.isEmpty()) {
                String firstViolation = violations.iterator().next().getMessage();
                throw new IllegalArgumentException(firstViolation);
            }

            if (request.getMessageType() == MessageType.TEXT
                    && (request.getContent() == null || request.getContent().trim().isEmpty())) {
                throw new IllegalArgumentException("Message content cannot be empty for TEXT messages.");
            }

            // Lưu DB
            WorldMessageResponse savedMessage = worldMessageService.postWorldMessage(
                    userId,
                    request.getContent(),
                    request.getMessageType(),
                    request.getStickerId());

            // Phát tán
            TCPMessage broadcastMsg = new TCPMessage("WORLD_CHAT_MESSAGE",
                    Map.of("message", savedMessage), this.username, null);

            for (ClientHandler client : onlineClients.values()) {
                client.sendMessage(broadcastMsg);
            }

        } catch (Exception e) {
            // Xử lý TẤT CẢ LỖI (Mapping, Validation, Service, Internal)
            Map<String, Object> errorMap = TcpErrorUtil.getErrorMap(e, username,
                    "Internal error during world chat processing.");
            sendMessage(new TCPMessage("ERROR", errorMap, "server", username));
        }
    }

    private void handlePrivateChat(TCPMessage message) {
        if (username == null || userId == null) {
            return;
        }
        PrivateMessageRequest request;
        try {
            request = objectMapper.convertValue(message.getData(), PrivateMessageRequest.class);

            // Validation
            Set<ConstraintViolation<PrivateMessageRequest>> violations = validator.validate(request);

            if (!violations.isEmpty()) {
                String reason = violations.iterator().next().getMessage();
                throw new IllegalArgumentException("Validation Error: " + reason);
            }

            String receiverUsername = message.getReceiver();

            if (receiverUsername == null) {
                throw new IllegalArgumentException("Missing receiver username in TCP message header.");
            }

            if (request.getMessageType() == MessageType.TEXT
                    && (request.getContent() == null || request.getContent().trim().isEmpty())) {
                throw new IllegalArgumentException("Message content cannot be empty for TEXT messages.");
            }

            // Lưu DB
            PrivateMessageResponse savedMessage = privateMessageService.sendPrivateMessage(
                    userId,
                    request.getToUserId(), 
                    request.getContent(),
                    null,
                    request.getMessageType(),
                    request.getStickerId());

            // Phát tán
            TCPMessage realtimeMsg = new TCPMessage("PRIVATE_CHAT_MESSAGE",
                    Map.of("message", savedMessage), this.username, receiverUsername);

            ClientHandler target = onlineClients.get(receiverUsername);
            if (target != null) {
                target.sendMessage(realtimeMsg);
            } else {
                log.info("[INFO] Private message saved for offline user: {}", receiverUsername);
            }

            sendMessage(realtimeMsg);

        } catch (Exception e) {
            Map<String, Object> errorMap = TcpErrorUtil.getErrorMap(e, username,
                    "Internal error during private chat processing.");
            sendMessage(new TCPMessage("ERROR", errorMap, "server", username));
        }
    }

    private void handleMatchChat(TCPMessage message) {
        if (username == null || userId == null) {
            return;
        }

        // Extract roomId trước khi map vào DTO
        String roomId = (String) message.getData().get("roomId");

        if (roomId == null) {
            sendMessage(new TCPMessage("ERROR", Map.of("reason", "Missing roomId"), "server", username));
            return;
        }

        MatchMessageRequest request;

        try {
            // Tạo bản sao data không có roomId để tránh lỗi "unrecognized field"
            Map<String, Object> dataForDTO = new java.util.HashMap<>(message.getData());
            dataForDTO.remove("roomId");
            
            request = objectMapper.convertValue(dataForDTO, MatchMessageRequest.class);

            // VALIDATION 
            Set<ConstraintViolation<MatchMessageRequest>> violations = validator.validate(request);

            if (!violations.isEmpty()) {
                String reason = violations.iterator().next().getMessage();
                throw new IllegalArgumentException("Validation Error: " + reason);
            }

            if (request.getMessageType() == MessageType.TEXT
                    && (request.getContent() == null || request.getContent().trim().isEmpty())) {
                throw new IllegalArgumentException("Message content cannot be empty for TEXT messages.");
            }

            // Kiểm tra tồn tại Room Session (RAM). Nếu chưa có, cố gắng khởi tạo từ DB Room.
            RoomSession session = RoomSessionManager.getRoom(roomId);
            if (session == null) {
                try {
                    // roomId trong TCP là String, DB là Long
                    Long rid = Long.parseLong(roomId);
                    com.ltm.memorygame.model.game.Room room = roomService.getEntityById(rid);

                    // Tạo session và add thành viên (host + guest nếu có)
                    session = RoomSessionManager.createRoom(roomId, room.getHost().getUsername());
                    session.addMember(room.getHost().getUsername());
                    if (room.getGuest() != null) {
                        session.addMember(room.getGuest().getUsername());
                    }
                    session.setActive(true);
                    log.info("[MATCH_CHAT] Created transient session for room {}", roomId);
                } catch (Exception ex) {
                    throw new IllegalStateException("Room not found or session expired.");
                }
            }

        // Đảm bảo sender là thành viên của RoomSession
        try { session.addMember(this.username); } catch (Exception ignored) {}

        // Gọi Service lưu RAM
            MatchMessageDTO matchMsgDTO = matchMessageService.sendMatchMessage(
                    roomId,
                    userId,
                    request.getContent(),
                    request.getMessageType(),
                    request.getStickerId());

            // Phát tán
            TCPMessage realtimeMsg = new TCPMessage("MATCH_CHAT_MESSAGE",
                    Map.of("message", matchMsgDTO), username, roomId);

            for (String memberUsername : session.getMembers()) {
                ClientHandler memberHandler = onlineClients.get(memberUsername);
                if (memberHandler != null) {
                    memberHandler.sendMessage(realtimeMsg);
                }
            }

        } catch (Exception e) {
            String defaultReason = "An unexpected error occurred.";
            Map<String, Object> errorMap = TcpErrorUtil.getErrorMap(e, username, defaultReason);
            sendMessage(new TCPMessage("ERROR", errorMap, "server", username));
        }
    }

    private void handleLobbyChat(TCPMessage message) {
        if (username == null || userId == null) {
            return;
        }

        // Kiểm tra RoomId
        Object roomIdObj = message.getData().get("roomId");
        if (roomIdObj == null) {
            sendMessage(new TCPMessage("ERROR", Map.of("reason", "Missing roomId"), "server", username));
            return;
        }

        Long roomId;
        try {
            roomId = Long.parseLong(roomIdObj.toString());
        } catch (NumberFormatException e) {
            sendMessage(new TCPMessage("ERROR", Map.of("reason", "Invalid roomId format"), "server", username));
            return;
        }

        try {
            // Lấy Room entity từ DB để biết host + guest
            Room room = roomService.getEntityById(roomId);
            if (room == null) {
                throw new IllegalStateException("Room not found: " + roomId);
            }

            // Validate request
            String content = (String) message.getData().get("content");
            if (content == null || content.trim().isEmpty()) {
                throw new IllegalArgumentException("Message content cannot be empty.");
            }

            String messageTypeStr = (String) message.getData().getOrDefault("messageType", "TEXT");
            MessageType messageType = MessageType.valueOf(messageTypeStr);
            String stickerId = (String) message.getData().get("stickerId");

            // Tạo message DTO đơn giản (không lưu DB, chỉ broadcast)
            Map<String, Object> lobbyMessage = new java.util.HashMap<>();
            lobbyMessage.put("id", java.util.UUID.randomUUID().toString());
            lobbyMessage.put("roomId", String.valueOf(roomId));
            lobbyMessage.put("content", content);
            lobbyMessage.put("messageType", messageType.name());
            if (stickerId != null) lobbyMessage.put("stickerId", stickerId);
            lobbyMessage.put("timestamp", System.currentTimeMillis());
            
            // Thêm sender info
            Map<String, Object> sender = new java.util.HashMap<>();
            sender.put("id", userId);
            sender.put("username", username);
            lobbyMessage.put("sender", sender);

            // Broadcast LOBBY_CHAT_MESSAGE cho host và guest
            TCPMessage realtimeMsg = new TCPMessage("LOBBY_CHAT_MESSAGE",
                    Map.of("message", lobbyMessage), username, String.valueOf(roomId));

            // Gửi cho host
            String hostUsername = room.getHost().getUsername();
            ClientHandler hostHandler = onlineClients.get(hostUsername);
            if (hostHandler != null) {
                hostHandler.sendMessage(realtimeMsg);
            }

            // Gửi cho guest (nếu có)
            if (room.getGuest() != null) {
                String guestUsername = room.getGuest().getUsername();
                ClientHandler guestHandler = onlineClients.get(guestUsername);
                if (guestHandler != null) {
                    guestHandler.sendMessage(realtimeMsg);
                }
            }

        } catch (Exception e) {
            String defaultReason = "An unexpected error occurred during lobby chat.";
            Map<String, Object> errorMap = TcpErrorUtil.getErrorMap(e, username, defaultReason);
            sendMessage(new TCPMessage("ERROR", errorMap, "server", username));
        }
    }

    private void updateAndBroadcastPresence(Long userId, UserStatus status) {
        try {
            // Lưu DB
            UserPresenceDTO dto = presenceService.setStatus(userId, status);

            // Phát tán
            TCPMessage msg = new TCPMessage("USER_PRESENCE_UPDATE",
                    Map.of("user", dto), "server", null);

            for (ClientHandler client : onlineClients.values()) {
                client.sendMessage(msg);
            }
        } catch (Exception e) {
            log.error("[PRESENCE] Failed to update and broadcast status for user {}: {}", username, e.getMessage(), e);
        }
    }

private void handleSetStatus(TCPMessage message) {
    if (username == null || userId == null)
        return;

    try {
        Object statusObj = message.getData().get("status");
        
        // Validation
        if (statusObj == null) {
            throw new IllegalArgumentException("Missing status parameter.");
        }

        UserStatus status = UserStatus.valueOf(statusObj.toString().toUpperCase());

        updateAndBroadcastPresence(this.userId, status);

    } catch (Exception e) {
        
        String defaultReason = "Failed to update status.";     
        Map<String, Object> errorMap = TcpErrorUtil.getErrorMap(e, username, defaultReason);
        sendMessage(new TCPMessage("ERROR", errorMap, "server", username));
    }
}

    private void broadcastUserStatus(String user, boolean online) {
        if (this.userId == null) {
            TCPMessage msg = new TCPMessage("USER_STATUS",
                    Map.of("user", user, "online", online), "server", null);
            for (ClientHandler client : onlineClients.values()) {
                client.sendMessage(msg);
            }
            return;
        }

        UserStatus status = online ? UserStatus.ONLINE : UserStatus.OFFLINE;

        //cập nhật DB và broadcast chi tiết
        updateAndBroadcastPresence(this.userId, status);
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
