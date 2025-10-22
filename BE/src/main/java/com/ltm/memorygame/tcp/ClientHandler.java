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

        // Kiểm tra RoomId
        String roomId = (String) message.getData().get("roomId");

        if (roomId == null) {
            sendMessage(new TCPMessage("ERROR", Map.of("reason", "Missing roomId"), "server", username));
            return;
        }

        MatchMessageRequest request;

        try {
            request = objectMapper.convertValue(message.getData(), MatchMessageRequest.class);

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

            // Kiểm tra tồn tại Room Session
            RoomSession session = RoomSessionManager.getRoom(roomId);
            if (session == null) {
                throw new IllegalStateException("Room not found or session expired.");
            }

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
