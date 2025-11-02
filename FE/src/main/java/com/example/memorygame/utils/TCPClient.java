package com.example.memorygame.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import com.example.memorygame.model.chat.ChatError;
import com.example.memorygame.model.chat.ChatMessage;
import com.example.memorygame.model.chat.ChatType;
import com.example.memorygame.model.chat.MessageStatus;
import com.example.memorygame.model.chat.MessageType;
import com.example.memorygame.model.user.UserSummary;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.application.Platform;

/**
 * TCP Client for real-time communication with the server.
 * Handles login, status updates, and chat messages.
 * 
 * Chat functionality:
 * - Map ChatMessage -> TCP payload and send to server
 * - Listen to incoming chat messages and map to ChatMessage
 * - Maintain recent-message cache per channel
 */
public class TCPClient {
    private static TCPClient instance;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private String host = "localhost"; // Default host
    private int port = 12345; // Default port (must match BE tcp.server.port)
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean running = false;
    
    private final Map<String, Consumer<TCPMessage>> messageHandlers = new ConcurrentHashMap<>();
    
    // Chat-specific fields
    private final Map<String, List<ChatMessage>> chatStore = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<ChatMessage>>> chatSubscribers = new ConcurrentHashMap<>();
    private final List<ChatMessageListener> chatListeners = new CopyOnWriteArrayList<>();
    
    private TCPClient() {
    }
    
    public static TCPClient getInstance() {
        if (instance == null) {
            synchronized (TCPClient.class) {
                if (instance == null) {
                    instance = new TCPClient();
                }
            }
        }
        return instance;
    }
    
    /**
     * Register a handler for a specific message type
     */
    public void onMessage(String messageType, Consumer<TCPMessage> handler) {
        messageHandlers.put(messageType, handler);
    }
    
    /**
     * Remove a handler for a specific message type
     */
    public void removeHandler(String messageType) {
        messageHandlers.remove(messageType);
    }
    
    /**
     * Connect and login to TCP server
     */
    public boolean connect(String username, String token) {
        try {
            if (socket != null && !socket.isClosed()) {
                System.out.println("[TCP] Already connected");
                return true;
            }
            
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            
            // Start listener thread
            running = true;
            Thread listenerThread = new Thread(this::listen, "TCP-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
            
            // Send login request
            Map<String, Object> loginData = new HashMap<>();
            loginData.put("username", username);
            loginData.put("token", token);
            
            TCPMessage loginMsg = new TCPMessage("LOGIN_REQUEST", loginData, username, null);
            sendMessage(loginMsg);
            
            System.out.println("[TCP] Connected to " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.err.println("[TCP] Connection failed: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Disconnect from server
     */
    public void disconnect() {
        running = false;
        try {
            if (out != null) {
                TCPMessage logoutMsg = new TCPMessage("LOGOUT_REQUEST", null, null, null);
                sendMessage(logoutMsg);
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("[TCP] Disconnected");
        } catch (IOException e) {
            System.err.println("[TCP] Error during disconnect: " + e.getMessage());
        }
    }
    
    /**
     * Send a message to the server
     */
    public void sendMessage(TCPMessage message) {
        if (out != null && socket != null && !socket.isClosed()) {
            try {
                String json = MAPPER.writeValueAsString(message);
                out.println(json);
            } catch (Exception e) {
                System.err.println("[TCP] Failed to send message: " + e.getMessage());
            }
        } else {
            System.err.println("[TCP] Not connected, cannot send message");
        }
    }
    
    /**
     * Listen for incoming messages from server
     */
    private void listen() {
        try {
            String line;
            while (running && (line = in.readLine()) != null) {
                try {
                    TCPMessage message = MAPPER.readValue(line, TCPMessage.class);
                    handleMessage(message);
                } catch (Exception e) {
                    System.err.println("[TCP] Failed to parse message: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("[TCP] Connection lost: " + e.getMessage());
            }
        } finally {
            running = false;
            try {
                if (socket != null) socket.close();
            } catch (IOException ignored) {}
        }
    }
    
    /**
     * Handle incoming message by calling registered handlers
     */
    private void handleMessage(TCPMessage message) {
        String type = message.getType();
        System.out.println("[TCP] Received: " + type + " | Data: " + message.getData());
        
        Consumer<TCPMessage> handler = messageHandlers.get(type);
        if (handler != null) {
            // Execute handler on JavaFX Application Thread
            Platform.runLater(() -> handler.accept(message));
        } else {
            System.out.println("[TCP] No handler registered for: " + type);
        }
    }
    
    public boolean isConnected() {
        return socket != null && !socket.isClosed() && running;
    }
    
    public void setHost(String host) {
        this.host = host;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    // ==================== Chat Methods ====================
    
    /**
     * Send a chat message to the server
     */
    public void sendChatMessage(ChatMessage message) {
        if (message == null || message.getChannelId() == null) return;

        try {
            String type = switch (message.getType()) {
                case WORLD -> "WORLD_CHAT";
                case PRIVATE -> "PRIVATE_CHAT";
                case MATCH -> "MATCH_CHAT";
                case LOBBY -> "LOBBY_CHAT";
                default -> "WORLD_CHAT";
            };

            // Build data map according to BE expectations
            Map<String, Object> data = new HashMap<>();
            data.put("content", message.getContent());
            data.put("messageType", message.getMessageType() != null ? message.getMessageType().name() : MessageType.TEXT.name());
            if (message.getStickerId() != null) data.put("stickerId", message.getStickerId());

            if (message.getType() == ChatType.MATCH || message.getType() == ChatType.LOBBY) {
                // Lấy roomId từ channelId (format: "match_123" hoặc "lobby_456")
                String channelId = message.getChannelId();
                String roomId = channelId;
                
                // Extract numeric roomId nếu có prefix
                if (channelId.contains("_")) {
                    String[] parts = channelId.split("_");
                    if (parts.length > 1) {
                        roomId = parts[1]; // Lấy phần sau dấu gạch dưới
                    }
                }
                
                data.put("roomId", roomId);
            }

            if (message.getType() == ChatType.PRIVATE && message.getReceiver() != null) {
                try {
                    data.put("toUserId", message.getReceiver().id);
                } catch (Exception ignored) {}
            }

            TCPMessage tcp = new TCPMessage(type, data,
                    message.getSender() != null ? message.getSender().username : null,
                    message.getType() == ChatType.PRIVATE && message.getReceiver() != null ? message.getReceiver().username : 
                    (message.getType() == ChatType.MATCH ? message.getChannelId() : null));

            sendMessage(tcp);

        } catch (Exception e) {
            notifyListenersOnError(new ChatError("Failed to send message", e));
        }
    }
    
    /**
     * Subscribe to chat messages for a specific channel
     */
    public void subscribeToChannel(String channelId, Consumer<ChatMessage> handler) {
        chatSubscribers.computeIfAbsent(channelId, k -> new ArrayList<>()).add(handler);
    }
    
    /**
     * Unsubscribe from a channel
     */
    public void unsubscribeFromChannel(String channelId) {
        chatSubscribers.remove(channelId);
    }
    
    /**
     * Get recent messages from a channel
     */
    public List<ChatMessage> getRecentMessages(String channelId, int limit) {
        List<ChatMessage> msgs = chatStore.get(channelId);
        if (msgs == null || msgs.isEmpty()) return Collections.emptyList();
        int from = Math.max(0, msgs.size() - limit);
        return new ArrayList<>(msgs.subList(from, msgs.size()));
    }
    
    /**
     * Add a listener for all chat events
     */
    public void addChatListener(ChatMessageListener listener) {
        if (listener != null) chatListeners.add(listener);
    }
    
    /**
     * Register handlers for incoming chat messages (called during initialization)
     */
    public void registerChatHandlers() {
        onMessage("WORLD_CHAT_MESSAGE", this::handleIncomingChat);
        onMessage("PRIVATE_CHAT_MESSAGE", this::handleIncomingChat);
        onMessage("MATCH_CHAT_MESSAGE", this::handleIncomingChat);
        onMessage("LOBBY_CHAT_MESSAGE", this::handleIncomingChat);
        // Optional: silence or handle presence updates to avoid noisy logs
        onMessage("USER_PRESENCE_UPDATE", msg -> {});
        // Handle server-side errors gracefully
        onMessage("ERROR", msg -> {
            System.err.println("[TCP] ERROR from server: " + msg.getData());
            notifyListenersOnError(new ChatError("Server error: " + String.valueOf(msg.getData()), null));
        });
    }
    
    private void handleIncomingChat(TCPMessage tcpMsg) {
        try {
            Map<String, Object> data = tcpMsg.getData();
            if (data == null) return;

            // BE often wraps payload in 'message' key
            Object payload = data.getOrDefault("message", data);
            if (!(payload instanceof Map)) return;
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) payload;
            
            // Debug log to see what fields are available
            System.out.println("[TCPClient] Incoming chat message fields: " + m.keySet());
            System.out.println("[TCPClient] tcpMsg.getSender(): " + tcpMsg.getSender());

            ChatMessage cm = mapToChatMessage(m, tcpMsg);
            if (cm == null) return;

            // Store and notify channel subscribers
            if (cm.getChannelId() != null) {
                chatStore.computeIfAbsent(cm.getChannelId(), k -> new ArrayList<>()).add(cm);
                notifyChannelSubscribers(cm.getChannelId(), cm);
            }

            notifyListenersOnMessage(cm);

        } catch (Exception e) {
            notifyListenersOnError(new ChatError("Failed to handle incoming TCP message", e));
        }
    }
    
    private ChatMessage mapToChatMessage(Map<String, Object> m, TCPMessage tcpMsg) {
        try {
            String id = safeGetString(m, "id");
            String content = safeGetString(m, "content");
            String msgTypeStr = safeGetString(m, "messageType");
            MessageType mt = MessageType.TEXT;
            try { if (msgTypeStr != null) mt = MessageType.valueOf(msgTypeStr); } catch (Exception ignored) {}

            String stickerId = safeGetString(m, "stickerId");
            String roomId = safeGetString(m, "roomId");
            String channelId = roomId != null ? roomId : safeGetString(m, "channelId");

            // sender
            UserSummary sender = null;
            Object senderObj = m.get("sender");
            if (senderObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> senderMap = (Map<String, Object>) senderObj;
                sender = mapToUserSummary(senderMap);
            } else {
                // For PRIVATE_CHAT_MESSAGE, create sender from fromUserId
                Object fromUserIdObj = m.get("fromUserId");
                if (fromUserIdObj instanceof Number) {
                    sender = new UserSummary();
                    sender.id = ((Number) fromUserIdObj).longValue();
                    
                    // Try to get username/displayName from various fields
                    Object usernameObj = m.get("username");
                    Object displayNameObj = m.get("displayName");
                    Object senderNameObj = m.get("senderName");
                    
                    if (displayNameObj instanceof String) {
                        sender.displayName = (String) displayNameObj;
                    } else if (senderNameObj instanceof String) {
                        sender.displayName = (String) senderNameObj;
                    } else if (usernameObj instanceof String) {
                        sender.username = (String) usernameObj;
                    } else {
                        // Fallback to tcpMsg.getSender()
                        String s = tcpMsg.getSender();
                        if (s != null) {
                            sender.username = s;
                        } else {
                            // Last resort: use "User" + id
                            sender.username = "User" + sender.id;
                        }
                    }
                } else {
                    // Fallback to tcpMsg.getSender()
                    String s = tcpMsg.getSender();
                    if (s != null) {
                        sender = new UserSummary();
                        sender.username = s;
                    }
                }
            }

            // timestamp
            LocalDateTime ts = LocalDateTime.now();
            Object tObj = m.get("timestamp");
            if (tObj instanceof Number) {
                ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) tObj).longValue()), ZoneId.systemDefault());
            } else if (tObj instanceof String) {
                try {
                    long v = Long.parseLong((String) tObj);
                    ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(v), ZoneId.systemDefault());
                } catch (NumberFormatException ignored) {}
            }

            // Infer ChatType from tcp message type
            ChatType chatType = ChatType.WORLD;
            String tcpType = tcpMsg.getType();
            if ("PRIVATE_CHAT_MESSAGE".equals(tcpType)) chatType = ChatType.PRIVATE;
            else if ("MATCH_CHAT_MESSAGE".equals(tcpType)) chatType = ChatType.MATCH;
            else if ("LOBBY_CHAT_MESSAGE".equals(tcpType)) chatType = ChatType.LOBBY;
            else if ("WORLD_CHAT_MESSAGE".equals(tcpType)) chatType = ChatType.WORLD;

            // Normalize channelId per chat type
            // Private: computed elsewhere from fromUserId/toUserId
            // Match: prefix with "match_" so it matches MatchChatContext channel subscription
            if ("MATCH_CHAT_MESSAGE".equals(tcpType)) {
                if (roomId != null && !roomId.isBlank()) {
                    channelId = "match_" + roomId;
                }
            }
            // Lobby: prefix with "lobby_" so it matches LobbyChatContext channel subscription
            if ("LOBBY_CHAT_MESSAGE".equals(tcpType)) {
                if (roomId != null && !roomId.isBlank()) {
                    channelId = "lobby_" + roomId;
                }
            }

            // For private chat, compute channelId from fromUserId and toUserId
            if ("PRIVATE_CHAT_MESSAGE".equals(tcpType) && channelId == null) {
                try {
                    Object fromUserIdObj = m.get("fromUserId");
                    Object toUserIdObj = m.get("toUserId");
                    if (fromUserIdObj instanceof Number && toUserIdObj instanceof Number) {
                        long id1 = Math.min(((Number) fromUserIdObj).longValue(), ((Number) toUserIdObj).longValue());
                        long id2 = Math.max(((Number) fromUserIdObj).longValue(), ((Number) toUserIdObj).longValue());
                        channelId = "private_" + id1 + "_" + id2;
                    }
                } catch (Exception ignored) {}
            }

            ChatMessage cm = new ChatMessage(id, content, sender, channelId, chatType);
            cm.setMessageType(mt);
            cm.setStickerId(stickerId);
            cm.setTimestamp(ts);
            cm.setStatus(MessageStatus.SENT);
            if (channelId == null) cm.setChannelId("world");

            return cm;
        } catch (Exception e) {
            return null;
        }
    }
    
    private UserSummary mapToUserSummary(Map<String, Object> m) {
        UserSummary u = new UserSummary();
        try {
            Object id = m.get("id");
            if (id instanceof Number) u.id = ((Number) id).longValue();
            else if (id instanceof String) u.id = Long.parseLong((String) id);
        } catch (Exception ignored) {}
        Object un = m.get("username"); if (un != null) u.username = un.toString();
        Object dn = m.get("displayName"); if (dn != null) u.displayName = dn.toString();
        Object av = m.get("avatarUrl"); if (av != null) u.avatarUrl = av.toString();
        Object st = m.get("status"); if (st != null) u.status = st.toString();
        try { Object ts = m.get("totalScore"); if (ts instanceof Number) u.totalScore = ((Number) ts).intValue(); } catch (Exception ignored) {}
        try { Object wc = m.get("winCount"); if (wc instanceof Number) u.winCount = ((Number) wc).intValue(); } catch (Exception ignored) {}
        return u;
    }
    
    private String safeGetString(Map<String, Object> m, String key) {
        Object o = m.get(key);
        return o == null ? null : o.toString();
    }
    
    private void notifyChannelSubscribers(String channelId, ChatMessage msg) {
        List<Consumer<ChatMessage>> subs = chatSubscribers.get(channelId);
        if (subs != null) {
            for (Consumer<ChatMessage> c : subs) {
                try { c.accept(msg); } catch (Exception e) { notifyListenersOnError(new ChatError("Subscriber error", e)); }
            }
        }
    }
    
    private void notifyListenersOnMessage(ChatMessage msg) {
        for (ChatMessageListener l : chatListeners) {
            try { l.onMessageReceived(msg); } catch (Exception ignored) {}
        }
    }
    
    private void notifyListenersOnError(ChatError err) {
        for (ChatMessageListener l : chatListeners) {
            try { l.onError(err); } catch (Exception ignored) {}
        }
    }
    
    /**
     * Chat message listener interface
     */
    public interface ChatMessageListener {
        void onMessageReceived(ChatMessage message);
        void onMessageStatusChanged(String messageId, MessageStatus status);
        void onError(ChatError error);
    }
    
    // ==================== TCP Message Structure ====================
    
    /**
     * TCP Message structure matching backend
     */
    public static class TCPMessage {
        private String type;
        private Map<String, Object> data;
        private String sender;
        private String receiver;
        private long timestamp;
        private String status;
        
        public TCPMessage() {
        }
        
        public TCPMessage(String type, Map<String, Object> data, String sender, String receiver) {
            this.type = type;
            this.data = data;
            this.sender = sender;
            this.receiver = receiver;
            this.timestamp = System.currentTimeMillis();
            this.status = "OK";
        }
        
        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public Map<String, Object> getData() { return data; }
        public void setData(Map<String, Object> data) { this.data = data; }
        
        public String getSender() { return sender; }
        public void setSender(String sender) { this.sender = sender; }
        
        public String getReceiver() { return receiver; }
        public void setReceiver(String receiver) { this.receiver = receiver; }
        
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}

