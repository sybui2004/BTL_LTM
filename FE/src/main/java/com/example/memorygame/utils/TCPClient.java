package com.example.memorygame.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
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
 * TCP Client để giao tiếp real-time với server
 * Xử lý đăng nhập, cập nhật trạng thái, và tin nhắn chat
 * 
 * Chức năng chat:
 * - Chuyển đổi ChatMessage -> TCP payload và gửi lên server
 * - Lắng nghe tin nhắn chat đến và chuyển đổi sang ChatMessage
 * - Duy trì cache tin nhắn gần đây theo từng channel
 */
public class TCPClient {
    private static TCPClient instance;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    private String host = "localhost";
    private int port = 12345;
    
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean running = false;
    
    private final Map<String, Consumer<TCPMessage>> messageHandlers = new ConcurrentHashMap<>();
    
    // Các field liên quan đến chat
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
     * Đăng ký handler cho một loại message cụ thể
     */
    public void onMessage(String messageType, Consumer<TCPMessage> handler) {
        messageHandlers.put(messageType, handler);
    }
    
    /**
     * Xóa handler cho một loại message
     */
    public void removeHandler(String messageType) {
        messageHandlers.remove(messageType);
    }
    
    /**
     * Kết nối và đăng nhập vào TCP server
     */
    public boolean connect(String username, String token) {
        try {
            if (socket != null && !socket.isClosed()) {
                System.out.println("[TCP] Already connected");
                return true;
            }
            
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            
            // Bắt đầu thread lắng nghe
            running = true;
            Thread listenerThread = new Thread(this::listen, "TCP-Listener");
            listenerThread.setDaemon(true);
            listenerThread.start();
            
            // Gửi yêu cầu đăng nhập
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
     * Ngắt kết nối với server
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
     * Gửi message lên server
     */
    public void sendMessage(TCPMessage message) {
        if (out != null && socket != null && !socket.isClosed()) {
            try {
                String json = MAPPER.writeValueAsString(message);
                out.println(json);
                out.flush();
            } catch (Exception e) {
                System.err.println("[TCP] Failed to send message: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("[TCP] Not connected, cannot send message");
        }
    }
    
    /**
     * Lắng nghe tin nhắn đến từ server
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
     * Xử lý tin nhắn đến bằng cách gọi các handler đã đăng ký
     */
    private void handleMessage(TCPMessage message) {
        String type = message.getType();
        
        Consumer<TCPMessage> handler = messageHandlers.get(type);
        if (handler != null) {
            // Thực thi handler trên JavaFX Application Thread
            Platform.runLater(() -> {
                try {
                    handler.accept(message);
                } catch (Exception e) {
                    System.err.println("[TCP] Error executing handler for " + type + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
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
     * Gửi tin nhắn chat lên server
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

            // Tạo data map theo yêu cầu của BE
            Map<String, Object> data = new HashMap<>();
            data.put("content", message.getContent());
            data.put("messageType", message.getMessageType() != null ? message.getMessageType().name() : MessageType.TEXT.name());
            if (message.getStickerId() != null) {
                try {
                    Long stickerIdLong = Long.parseLong(message.getStickerId());
                    data.put("stickerId", stickerIdLong);
                } catch (NumberFormatException e) {
                    System.err.println("[TCPClient] Invalid stickerId format: " + message.getStickerId());
                    return;
                }
            }

            if (message.getType() == ChatType.MATCH || message.getType() == ChatType.LOBBY) {
                // Lấy roomId từ channelId (format: "match_123" hoặc "lobby_456")
                String channelId = message.getChannelId();
                String roomId = channelId;
                
                if (channelId.contains("_")) {
                    String[] parts = channelId.split("_");
                    if (parts.length > 1) {
                        roomId = parts[1];
                    }
                }
                
                data.put("roomId", roomId);
            }

            if (message.getType() == ChatType.PRIVATE && message.getReceiver() != null) {
                try {
                    data.put("toUserId", message.getReceiver().id);
                } catch (Exception ignored) {}
            }

            String receiverUsername = null;
            if (message.getType() == ChatType.PRIVATE && message.getReceiver() != null) {
                receiverUsername = message.getReceiver().username;
                
                if (receiverUsername == null || receiverUsername.trim().isEmpty()) {
                    try {
                        com.example.memorygame.model.user.UserSummary fullUser = 
                            com.example.memorygame.utils.UserApi.getUserById(message.getReceiver().id);
                        if (fullUser != null && fullUser.username != null) {
                            receiverUsername = fullUser.username;
                        }
                    } catch (Exception ignored) {}
                }
            } else if (message.getType() == ChatType.MATCH) {
                receiverUsername = message.getChannelId();
            }

            TCPMessage tcp = new TCPMessage(type, data,
                    message.getSender() != null ? message.getSender().username : null,
                    receiverUsername);

            sendMessage(tcp);

        } catch (Exception e) {
            notifyListenersOnError(new ChatError("Failed to send message", e));
        }
    }
    
    /**
     * Đăng ký nhận tin nhắn chat cho một channel cụ thể
     */
    public void subscribeToChannel(String channelId, Consumer<ChatMessage> handler) {
        chatSubscribers.computeIfAbsent(channelId, k -> new ArrayList<>()).add(handler);
    }
    
    /**
     * Hủy đăng ký nhận tin nhắn từ một channel
     */
    public void unsubscribeFromChannel(String channelId) {
        chatSubscribers.remove(channelId);
    }
    
    /**
     * Lấy tin nhắn gần đây từ một channel
     * Trả về tin nhắn đã sắp xếp theo timestamp (cũ nhất đến mới nhất)
     */
    public List<ChatMessage> getRecentMessages(String channelId, int limit) {
        List<ChatMessage> msgs = chatStore.get(channelId);
        if (msgs == null || msgs.isEmpty()) return Collections.emptyList();
        
        // Sắp xếp theo timestamp để đảm bảo thứ tự đúng
        List<ChatMessage> sorted = new ArrayList<>(msgs);
        sorted.sort((m1, m2) -> {
            if (m1.getTimestamp() == null && m2.getTimestamp() == null) return 0;
            if (m1.getTimestamp() == null) return -1;
            if (m2.getTimestamp() == null) return 1;
            return m1.getTimestamp().compareTo(m2.getTimestamp());
        });
        
        // Lấy N tin nhắn gần nhất
        int from = Math.max(0, sorted.size() - limit);
        return new ArrayList<>(sorted.subList(from, sorted.size()));
    }
    
    /**
     * Thêm listener cho tất cả sự kiện chat
     */
    public void addChatListener(ChatMessageListener listener) {
        if (listener != null) chatListeners.add(listener);
    }
    
    /**
     * Đăng ký handlers cho tin nhắn chat đến (được gọi khi khởi tạo)
     */
    public void registerChatHandlers() {
        onMessage("WORLD_CHAT_MESSAGE", this::handleIncomingChat);
        onMessage("PRIVATE_CHAT_MESSAGE", this::handleIncomingChat);
        onMessage("MATCH_CHAT_MESSAGE", this::handleIncomingChat);
        onMessage("LOBBY_CHAT_MESSAGE", this::handleIncomingChat);
        onMessage("USER_PRESENCE_UPDATE", msg -> {});
        onMessage("ERROR", msg -> {
            System.err.println("[TCP] ERROR from server: " + msg.getData());
            notifyListenersOnError(new ChatError("Server error: " + String.valueOf(msg.getData()), null));
        });
    }
    
    /**
     * Xử lý tin nhắn chat đến từ server
     */
    private void handleIncomingChat(TCPMessage tcpMsg) {
        try {
            Map<String, Object> data = tcpMsg.getData();
            if (data == null) {
                return;
            }

            // BE thường bọc payload trong key 'message'
            Object payload = data.getOrDefault("message", data);
            if (!(payload instanceof Map)) {
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> m = (Map<String, Object>) payload;

            ChatMessage cm = mapToChatMessage(m, tcpMsg);
            if (cm == null) return;

            // Lưu vào store và thông báo cho subscribers
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
            String stickerPath = null;
            
            // Lấy sticker path từ sticker object nếu có
            Object stickerObj = m.get("sticker");
            if (stickerObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stickerMap = (Map<String, Object>) stickerObj;
                Object stickerPathObj = stickerMap.get("stickerPath");
                if (stickerPathObj != null) {
                    stickerPath = stickerPathObj.toString();
                }
                if (stickerId == null) {
                    Object idObj = stickerMap.get("id");
                    if (idObj != null) {
                        stickerId = idObj.toString();
                    }
                }
            }
            
            String roomId = safeGetString(m, "roomId");
            String channelId = roomId != null ? roomId : safeGetString(m, "channelId");

            // Xử lý thông tin người gửi
            UserSummary sender = null;
            Object senderObj = m.get("sender");
            if (senderObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> senderMap = (Map<String, Object>) senderObj;
                sender = mapToUserSummary(senderMap);
            } else {
                // Với WORLD_CHAT_MESSAGE, lấy từ senderId và senderName
                Object senderIdObj = m.get("senderId");
                if (senderIdObj instanceof Number) {
                    sender = new UserSummary();
                    sender.id = ((Number) senderIdObj).longValue();
                    
                    Object usernameObj = m.get("username");
                    Object displayNameObj = m.get("displayName");
                    Object senderNameObj = m.get("senderName");
                    Object avatarUrlObj = m.get("avatarUrl");
                    
                    // Ưu tiên displayName/senderName
                    if (displayNameObj instanceof String && !((String) displayNameObj).isBlank()) {
                        sender.displayName = (String) displayNameObj;
                    } else if (senderNameObj instanceof String && !((String) senderNameObj).isBlank()) {
                        sender.displayName = (String) senderNameObj;
                    }
                    
                    // Lấy username nếu có
                    if (usernameObj instanceof String && !((String) usernameObj).isBlank()) {
                        sender.username = (String) usernameObj;
                    } else {
                        // Fallback: dùng displayName làm username nếu không có username
                        if (sender.displayName != null && !sender.displayName.isBlank()) {
                            sender.username = sender.displayName;
                        } else {
                            String s = tcpMsg.getSender();
                            if (s != null) {
                                sender.username = s;
                            } else {
                                sender.username = "User" + sender.id;
                            }
                        }
                    }
                    
                    if (avatarUrlObj != null) {
                        sender.avatarUrl = avatarUrlObj.toString();
                    }
                } else {
                    // Với PRIVATE_CHAT_MESSAGE, tạo sender từ fromUserId
                    Object fromUserIdObj = m.get("fromUserId");
                    if (fromUserIdObj instanceof Number) {
                        sender = new UserSummary();
                        sender.id = ((Number) fromUserIdObj).longValue();
                        
                        Object usernameObj = m.get("username");
                        Object displayNameObj = m.get("displayName");
                        Object senderNameObj = m.get("senderName");
                        Object avatarUrlObj = m.get("avatarUrl");
                        
                        // Ưu tiên displayName từ displayName hoặc senderName
                        if (displayNameObj instanceof String && !((String) displayNameObj).isBlank()) {
                            sender.displayName = (String) displayNameObj;
                        } else if (senderNameObj instanceof String && !((String) senderNameObj).isBlank()) {
                            sender.displayName = (String) senderNameObj;
                        }
                        
                        // Set username
                        if (usernameObj instanceof String && !((String) usernameObj).isBlank()) {
                            sender.username = (String) usernameObj;
                        } else {
                            String s = tcpMsg.getSender();
                            if (s != null) {
                                sender.username = s;
                            } else {
                                sender.username = "User" + sender.id;
                            }
                        }
                        
                        // Set avatarUrl
                        if (avatarUrlObj != null && !avatarUrlObj.toString().isBlank() && !"null".equals(avatarUrlObj.toString())) {
                            sender.avatarUrl = avatarUrlObj.toString();
                        }
                    } else {
                        String s = tcpMsg.getSender();
                        if (s != null) {
                            sender = new UserSummary();
                            sender.username = s;
                        }
                    }
                }
            }

            // Timestamp (same logic as ChatApi for PrivateChat and WorldChat)
            // Backend gửi qua TCP dùng "timestamp", nhưng cũng thử "createdAt" để đảm bảo tương thích
            LocalDateTime ts = null;
            Object tObj = m.get("timestamp");
            if (tObj == null) {
                tObj = m.get("createdAt"); // Fallback nếu không có "timestamp"
            }
            
            if (tObj instanceof Number) {
                long epochMillis = ((Number) tObj).longValue();
                ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
            } else if (tObj instanceof Map) {
                // Jackson may serialize Instant as {epochSecond: xxx, nano: yyy}
                @SuppressWarnings("unchecked")
                Map<String, Object> instantMap = (Map<String, Object>) tObj;
                Object epochSecondObj = instantMap.get("epochSecond");
                Object nanoObj = instantMap.get("nano");
                if (epochSecondObj instanceof Number) {
                    long epochSecond = ((Number) epochSecondObj).longValue();
                    int nano = (nanoObj instanceof Number) ? ((Number) nanoObj).intValue() : 0;
                    ts = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond, nano), ZoneId.systemDefault());
                }
            } else if (tObj instanceof String) {
                // Try parsing as ISO string or epoch millis string
                String timeStr = (String) tObj;
                try {
                    // Try epoch millis first
                    long epochMillis = Long.parseLong(timeStr);
                    ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
                } catch (NumberFormatException e) {
                    // Try ISO format (e.g., "2025-11-08T03:34:31.168Z")
                    try {
                        Instant instant = Instant.parse(timeStr);
                        ts = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    } catch (Exception e2) {
                        System.err.println("[TCPClient] ERROR: Failed to parse timestamp string: " + timeStr + ", error: " + e2.getMessage());
                    }
                }
            } else if (tObj != null) {
                System.err.println("[TCPClient] WARNING: Unknown timestamp type: " + tObj.getClass().getName() + ", value: " + tObj);
            }
            
            if (ts == null) {
                System.err.println("[TCPClient] WARNING: Failed to parse timestamp, using now() as fallback");
                ts = LocalDateTime.now();
            }

            // Xác định ChatType từ loại TCP message
            ChatType chatType = ChatType.WORLD;
            String tcpType = tcpMsg.getType();
            if ("PRIVATE_CHAT_MESSAGE".equals(tcpType)) chatType = ChatType.PRIVATE;
            else if ("MATCH_CHAT_MESSAGE".equals(tcpType)) chatType = ChatType.MATCH;
            else if ("LOBBY_CHAT_MESSAGE".equals(tcpType)) chatType = ChatType.LOBBY;
            else if ("WORLD_CHAT_MESSAGE".equals(tcpType)) chatType = ChatType.WORLD;

            // Chuẩn hóa channelId theo loại chat
            if ("MATCH_CHAT_MESSAGE".equals(tcpType)) {
                if (roomId != null && !roomId.isBlank()) {
                    channelId = "match_" + roomId;
                }
            }
            if ("LOBBY_CHAT_MESSAGE".equals(tcpType)) {
                if (roomId != null && !roomId.isBlank()) {
                    channelId = "lobby_" + roomId;
                }
            }

            // Với private chat, tính channelId từ fromUserId và toUserId
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
            cm.setStickerPath(stickerPath);
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
     * Interface listener cho sự kiện chat message
     */
    public interface ChatMessageListener {
        void onMessageReceived(ChatMessage message);
        void onMessageStatusChanged(String messageId, MessageStatus status);
        void onError(ChatError error);
    }
    
    // ==================== TCP Message Structure ====================
    
    /**
     * Cấu trúc TCP Message khớp với backend
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

