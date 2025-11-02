package com.example.memorygame.utils;

import com.example.memorygame.model.chat.ChatMessage;
import com.example.memorygame.model.chat.ChatType;
import com.example.memorygame.model.chat.MessageType;
import com.example.memorygame.model.user.UserSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API client for chat operations (world, private, lobby, match history)
 */
public class ChatApi {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Fetch world chat history (recent 100 messages by default)
     * GET /api/chat/world/?page=0&size=100
     * 
     * @return List of ChatMessage for world channel (channelId="world")
     */
    public static List<ChatMessage> fetchWorldHistory() {
        return fetchWorldHistory(0, 100);
    }

    /**
     * Fetch world chat history with pagination
     * 
     * @param page page number (0-based)
     * @param size messages per page (max 100)
     * @return List of ChatMessage for world channel
     */
    public static List<ChatMessage> fetchWorldHistory(int page, int size) {
        try {
            String json = ApiClient.getAuth("/api/chat/world/?page=" + page + "&size=" + size);
            // BE returns Page<WorldMessageResponse>; extract content array
            Map<String, Object> pageData = MAPPER.readValue(json, new TypeReference<Map<String, Object>>(){});
            Object contentObj = pageData.get("content");
            if (contentObj == null) return Collections.emptyList();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentObj;

            // BE trả về DESC (mới nhất trước), reverse để hiển thị cũ nhất trước
            List<ChatMessage> messages = contentList.stream()
                    .map(ChatApi::mapWorldMessageToChatMessage)
                    .collect(Collectors.toList());
            Collections.reverse(messages);
            return messages;
        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to fetch world history: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch private chat history between current user and another user
     * GET /api/chat/private/{otherUserId}?page=0&size=100
     * 
     * @param otherUserId ID của người chat cùng
     * @return List of ChatMessage for private channel
     */
    public static List<ChatMessage> fetchPrivateHistory(long otherUserId) {
        return fetchPrivateHistory(otherUserId, 0, 100);
    }

    /**
     * Fetch private chat history with pagination
     * 
     * @param otherUserId ID của người chat cùng
     * @param page page number (0-based)
     * @param size messages per page (max 100)
     * @return List of ChatMessage for private channel
     */
    public static List<ChatMessage> fetchPrivateHistory(long otherUserId, int page, int size) {
        try {
            // BE expects userId in header; ApiClient doesn't support custom headers yet
            // For now, use path param approach or extend ApiClient
            // Assuming BE allows userId from JWT token in Authorization header
            String json = ApiClient.getAuth("/api/chat/private/" + otherUserId + "?page=" + page + "&size=" + size);
            
            // BE returns Page<PrivateMessageResponse>; extract content array
            Map<String, Object> pageData = MAPPER.readValue(json, new TypeReference<Map<String, Object>>(){});
            Object contentObj = pageData.get("content");
            if (contentObj == null) return Collections.emptyList();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentObj;

            // BE trả về ASC (cũ nhất trước), reverse để hiển thị mới nhất trước
            List<ChatMessage> messages = contentList.stream()
                    .map(m -> mapPrivateMessageToChatMessage(m, otherUserId))
                    .collect(Collectors.toList());
            Collections.reverse(messages);
            return messages;
        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to fetch private history: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch conversation list (users với tin nhắn cuối cùng)
     * GET /api/chat/private/conversations/{userId}
     * 
     * @param currentUserId ID của user hiện tại
     * @return List of ConversationPreview (otherUser info + last message)
     */
    public static List<ConversationPreview> fetchConversationList(long currentUserId) {
        try {
            String json = ApiClient.getAuth("/api/chat/private/conversations/" + currentUserId);
            List<Map<String, Object>> conversations = MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>(){});
            
            return conversations.stream()
                    .map(ChatApi::mapConversationPreview)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to fetch conversation list: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Map WorldMessageResponse to ChatMessage
     */
    private static ChatMessage mapWorldMessageToChatMessage(Map<String, Object> m) {
        try {
            String id = String.valueOf(m.getOrDefault("id", ""));
            String content = String.valueOf(m.getOrDefault("content", ""));
            
            // Sender
            UserSummary sender = new UserSummary();
            Object senderIdObj = m.get("senderId");
            if (senderIdObj instanceof Number) {
                sender.id = ((Number) senderIdObj).longValue();
            }
            sender.displayName = String.valueOf(m.getOrDefault("senderName", ""));
            sender.username = sender.displayName; // fallback
            Object avatarObj = m.get("avatarUrl");
            if (avatarObj != null && !avatarObj.toString().isBlank()) {
                sender.avatarUrl = avatarObj.toString();
            }

            // MessageType
            String msgTypeStr = String.valueOf(m.getOrDefault("messageType", "TEXT"));
            MessageType mt = MessageType.TEXT;
            try { mt = MessageType.valueOf(msgTypeStr); } catch (Exception ignored) {}

            // Sticker (optional)
            String stickerId = null;
            Object stickerObj = m.get("sticker");
            if (stickerObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stickerMap = (Map<String, Object>) stickerObj;
                Object stickerIdObj = stickerMap.get("id");
                if (stickerIdObj != null) stickerId = stickerIdObj.toString();
            }

            // Timestamp (createdAt from BE is epoch millis)
            LocalDateTime ts = LocalDateTime.now();
            Object createdAtObj = m.get("createdAt");
            if (createdAtObj instanceof Number) {
                ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) createdAtObj).longValue()), ZoneId.systemDefault());
            }

            ChatMessage cm = new ChatMessage(id, content, sender, "world", ChatType.WORLD);
            cm.setMessageType(mt);
            cm.setStickerId(stickerId);
            cm.setTimestamp(ts);
            return cm;

        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to parse world message: " + e.getMessage());
            return null;
        }
    }

    /**
     * Map PrivateMessageResponse to ChatMessage
     */
    private static ChatMessage mapPrivateMessageToChatMessage(Map<String, Object> m, long otherUserId) {
        try {
            String id = String.valueOf(m.getOrDefault("id", ""));
            String content = String.valueOf(m.getOrDefault("content", ""));

            // Sender & Receiver IDs
            Long fromUserId = null;
            Object fromObj = m.get("fromUserId");
            if (fromObj instanceof Number) fromUserId = ((Number) fromObj).longValue();

            Long toUserId = null;
            Object toObj = m.get("toUserId");
            if (toObj instanceof Number) toUserId = ((Number) toObj).longValue();

            // Determine sender (fromUserId) and fetch full user info
            // BE doesn't return full sender details in PrivateMessageResponse, so fetch from UserApi
            UserSummary sender = new UserSummary();
            if (fromUserId != null) {
                sender.id = fromUserId;
                // Fetch full user info to get displayName/username
                try {
                    UserSummary fullSender = com.example.memorygame.utils.UserApi.getUserById(fromUserId);
                    if (fullSender != null) {
                        sender = fullSender;
                    } else {
                        // Fallback: use ID only, displayName will be empty
                        sender.username = "User#" + fromUserId;
                    }
                } catch (Exception e) {
                    // Fallback on fetch error
                    sender.username = "User#" + fromUserId;
                }
            }

            // MessageType
            String msgTypeStr = String.valueOf(m.getOrDefault("messageType", "TEXT"));
            MessageType mt = MessageType.TEXT;
            try { mt = MessageType.valueOf(msgTypeStr); } catch (Exception ignored) {}

            // Sticker (optional)
            String stickerId = null;
            Object stickerObj = m.get("sticker");
            if (stickerObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stickerMap = (Map<String, Object>) stickerObj;
                Object stickerIdObj = stickerMap.get("id");
                if (stickerIdObj != null) stickerId = stickerIdObj.toString();
            }

            // Timestamp (createdAt from BE is epoch millis)
            LocalDateTime ts = LocalDateTime.now();
            Object createdAtObj = m.get("createdAt");
            if (createdAtObj instanceof Number) {
                ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) createdAtObj).longValue()), ZoneId.systemDefault());
            }

            // Generate channelId for private chat (sorted user IDs)
            long id1 = Math.min(fromUserId != null ? fromUserId : 0, toUserId != null ? toUserId : 0);
            long id2 = Math.max(fromUserId != null ? fromUserId : 0, toUserId != null ? toUserId : 0);
            String channelId = "private_" + id1 + "_" + id2;

            ChatMessage cm = new ChatMessage(id, content, sender, channelId, ChatType.PRIVATE);
            cm.setMessageType(mt);
            cm.setStickerId(stickerId);
            cm.setTimestamp(ts);

            // Set receiver if needed (for private chat display)
            if (toUserId != null) {
                UserSummary receiver = new UserSummary();
                receiver.id = toUserId;
                cm.setReceiver(receiver);
            }

            return cm;

        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to parse private message: " + e.getMessage());
            return null;
        }
    }

    /**
     * Map ConversationPreviewDTO to ConversationPreview model
     */
    private static ConversationPreview mapConversationPreview(Map<String, Object> m) {
        try {
            ConversationPreview conv = new ConversationPreview();

            Object otherUserIdObj = m.get("otherUserId");
            if (otherUserIdObj instanceof Number) conv.otherUserId = ((Number) otherUserIdObj).longValue();

            conv.otherUsername = String.valueOf(m.getOrDefault("otherUsername", ""));
            conv.otherDisplayName = String.valueOf(m.getOrDefault("otherDisplayName", ""));
            
            Object avatarObj = m.get("otherAvatarUrl");
            if (avatarObj != null && !avatarObj.toString().isBlank()) {
                conv.otherAvatarUrl = avatarObj.toString();
            }

            Object lastMsgIdObj = m.get("lastMessageId");
            if (lastMsgIdObj instanceof Number) conv.lastMessageId = ((Number) lastMsgIdObj).longValue();

            conv.lastMessageText = String.valueOf(m.getOrDefault("lastMessageText", ""));

            String msgTypeStr = String.valueOf(m.getOrDefault("lastMessageType", "TEXT"));
            try { conv.lastMessageType = MessageType.valueOf(msgTypeStr); } catch (Exception ignored) { conv.lastMessageType = MessageType.TEXT; }

            Object lastMsgTimeObj = m.get("lastMessageTime");
            if (lastMsgTimeObj instanceof Number) {
                conv.lastMessageTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) lastMsgTimeObj).longValue()), ZoneId.systemDefault());
            }

            Object fromSelfObj = m.get("lastMessageFromSelf");
            if (fromSelfObj instanceof Boolean) conv.lastMessageFromSelf = (Boolean) fromSelfObj;

            return conv;

        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to parse conversation preview: " + e.getMessage());
            return null;
        }
    }

    /**
     * Simple model for conversation preview (list of users with last message)
     */
    public static class ConversationPreview {
        public long otherUserId;
        public String otherUsername;
        public String otherDisplayName;
        public String otherAvatarUrl;
        public long lastMessageId;
        public String lastMessageText;
        public MessageType lastMessageType;
        public LocalDateTime lastMessageTime;
        public boolean lastMessageFromSelf;

        public String getDisplayName() {
            return (otherDisplayName != null && !otherDisplayName.isBlank()) ? otherDisplayName : otherUsername;
        }
    }
}
