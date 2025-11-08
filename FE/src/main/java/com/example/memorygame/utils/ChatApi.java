package com.example.memorygame.utils;

import com.example.memorygame.model.chat.ChatMessage;
import com.example.memorygame.model.chat.ChatType;
import com.example.memorygame.model.chat.MessageType;
import com.example.memorygame.model.chat.Sticker;
import com.example.memorygame.model.user.UserSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
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
     * Fetch world chat history (recent 100 messages)
     * GET /api/chat/world/
     * 
     * @return List of ChatMessage for world channel (channelId="world")
     */
    public static List<ChatMessage> fetchWorldHistory() {
        try {
            String json = ApiClient.getAuth("/api/chat/world/");
            // BE returns List<WorldMessageResponse> directly
            List<Map<String, Object>> contentList = MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>(){});

            // BE đã trả về đúng thứ tự ASC (cũ nhất trước, mới nhất cuối)
            // Không cần reverse nữa - giữ nguyên thứ tự từ BE
            List<ChatMessage> messages = contentList.stream()
                    .map(ChatApi::mapWorldMessageToChatMessage)
                    .collect(Collectors.toList());
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
            String json = ApiClient.getAuth("/api/chat/private/" + otherUserId + "?page=" + page + "&size=" + size);
            
            // BE returns Page<PrivateMessageResponse>; extract content array
            Map<String, Object> pageData = MAPPER.readValue(json, new TypeReference<Map<String, Object>>(){});
            Object contentObj = pageData.get("content");
            if (contentObj == null) return Collections.emptyList();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentObj;

            // BE trả về ASC (cũ nhất trước), giữ nguyên thứ tự để ChatComponent tự sort
            List<ChatMessage> messages = contentList.stream()
                    .map(m -> mapPrivateMessageToChatMessage(m, otherUserId))
                    .filter(msg -> msg != null)
                    .collect(Collectors.toList());
            return messages;
        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to fetch private history: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * Fetch all private chat history by fetching multiple pages if needed
     * 
     * @param otherUserId ID của người chat cùng
     * @return List of ChatMessage for private channel (all messages)
     */
    public static List<ChatMessage> fetchAllPrivateHistory(long otherUserId) {
        List<ChatMessage> allMessages = new ArrayList<>();
        int page = 0;
        int size = 100; // Maximum size per request
        
        try {
            while (true) {
                String json = ApiClient.getAuth("/api/chat/private/" + otherUserId + "?page=" + page + "&size=" + size);
                
                Map<String, Object> pageData = MAPPER.readValue(json, new TypeReference<Map<String, Object>>(){});
                Object contentObj = pageData.get("content");
                
                if (contentObj == null) break;
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentObj;
                
                if (contentList.isEmpty()) break;
                
                List<ChatMessage> pageMessages = contentList.stream()
                        .map(m -> mapPrivateMessageToChatMessage(m, otherUserId))
                        .filter(msg -> msg != null)
                        .collect(Collectors.toList());
                
                allMessages.addAll(pageMessages);
                
                // Check if there are more pages
                Object totalPagesObj = pageData.get("totalPages");
                Object totalElementsObj = pageData.get("totalElements");
                boolean hasMore = false;
                
                if (totalPagesObj instanceof Number) {
                    int totalPages = ((Number) totalPagesObj).intValue();
                    hasMore = (page + 1) < totalPages;
                } else if (totalElementsObj instanceof Number) {
                    // If we got less than requested, we're done
                    hasMore = pageMessages.size() >= size;
                } else {
                    // Fallback: if we got a full page, try next page
                    hasMore = pageMessages.size() >= size;
                }
                
                if (!hasMore) break;
                page++;
            }
            
            // BE trả về ASC (cũ nhất trước), nhưng trong ChatComponent sẽ sort lại
            // Không reverse ở đây, để ChatComponent tự sort theo timestamp
            System.out.println("[ChatApi] Fetched " + allMessages.size() + " messages from history");
            return allMessages;
        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to fetch all private history: " + e.getMessage());
            e.printStackTrace();
            return allMessages; // Return what we have so far
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
                    .filter(conv -> conv != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to fetch conversation list: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Fetch all friends with conversation info (including those without messages)
     * GET /api/chat/private/friends-with-conversations/{userId}
     * 
     * @param currentUserId ID của user hiện tại
     * @return List of ConversationPreview (all friends + conversations)
     */
    public static List<ConversationPreview> fetchFriendsWithConversations(long currentUserId) {
        try {
            String json = ApiClient.getAuth("/api/chat/private/friends-with-conversations/" + currentUserId);
            List<Map<String, Object>> conversations = MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>(){});
            
            return conversations.stream()
                    .map(ChatApi::mapConversationPreview)
                    .filter(conv -> conv != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to fetch friends with conversations: " + e.getMessage());
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
            String stickerPath = null;
            Object stickerObj = m.get("sticker");
            if (stickerObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stickerMap = (Map<String, Object>) stickerObj;
                Object stickerIdObj = stickerMap.get("id");
                if (stickerIdObj != null) stickerId = stickerIdObj.toString();
                Object stickerPathObj = stickerMap.get("stickerPath");
                if (stickerPathObj != null) stickerPath = stickerPathObj.toString();
            }

            // Timestamp (createdAt from BE can be epoch millis, Instant object, or string) - same logic as private messages
            LocalDateTime ts = null;
            Object createdAtObj = m.get("createdAt");
            
            if (createdAtObj instanceof Number) {
                ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) createdAtObj).longValue()), ZoneId.systemDefault());
            } else if (createdAtObj instanceof Map) {
                // Jackson may serialize Instant as {epochSecond: xxx, nano: yyy}
                @SuppressWarnings("unchecked")
                Map<String, Object> instantMap = (Map<String, Object>) createdAtObj;
                Object epochSecondObj = instantMap.get("epochSecond");
                Object nanoObj = instantMap.get("nano");
                if (epochSecondObj instanceof Number) {
                    long epochSecond = ((Number) epochSecondObj).longValue();
                    int nano = (nanoObj instanceof Number) ? ((Number) nanoObj).intValue() : 0;
                    ts = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond, nano), ZoneId.systemDefault());
                }
            } else if (createdAtObj instanceof String) {
                // Try parsing as ISO string or epoch millis string
                String timeStr = (String) createdAtObj;
                try {
                    // Try epoch millis first
                    long epochMillis = Long.parseLong(timeStr);
                    ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
                } catch (NumberFormatException e) {
                    // Try ISO format (e.g., "2025-11-08T03:34:31.168Z")
                    try {
                        Instant instant = Instant.parse(timeStr);
                        ts = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    } catch (Exception ignored) {}
                }
            }
            
            if (ts == null) {
                ts = LocalDateTime.now();
            }

            ChatMessage cm = new ChatMessage(id, content, sender, "world", ChatType.WORLD);
            cm.setMessageType(mt);
            cm.setStickerId(stickerId);
            cm.setStickerPath(stickerPath);
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
            String stickerPath = null;
            Object stickerObj = m.get("sticker");
            if (stickerObj instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> stickerMap = (Map<String, Object>) stickerObj;
                Object stickerIdObj = stickerMap.get("id");
                if (stickerIdObj != null) stickerId = stickerIdObj.toString();
                Object stickerPathObj = stickerMap.get("stickerPath");
                if (stickerPathObj != null) stickerPath = stickerPathObj.toString();
            }

            // Timestamp (createdAt from BE can be epoch millis, Instant object, or string)
            LocalDateTime ts = null;
            Object createdAtObj = m.get("createdAt");
            
            if (createdAtObj instanceof Number) {
                ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) createdAtObj).longValue()), ZoneId.systemDefault());
            } else if (createdAtObj instanceof Map) {
                // Jackson may serialize Instant as {epochSecond: xxx, nano: yyy}
                @SuppressWarnings("unchecked")
                Map<String, Object> instantMap = (Map<String, Object>) createdAtObj;
                Object epochSecondObj = instantMap.get("epochSecond");
                Object nanoObj = instantMap.get("nano");
                if (epochSecondObj instanceof Number) {
                    long epochSecond = ((Number) epochSecondObj).longValue();
                    int nano = (nanoObj instanceof Number) ? ((Number) nanoObj).intValue() : 0;
                    ts = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSecond, nano), ZoneId.systemDefault());
                }
            } else if (createdAtObj instanceof String) {
                // Try parsing as ISO string or epoch millis string
                String timeStr = (String) createdAtObj;
                try {
                    // Try epoch millis first
                    long epochMillis = Long.parseLong(timeStr);
                    ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
                } catch (NumberFormatException e) {
                    // Try ISO format (e.g., "2025-11-08T03:34:31.168Z")
                    try {
                        Instant instant = Instant.parse(timeStr);
                        ts = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    } catch (Exception ignored) {}
                }
            }
            
            if (ts == null) {
                ts = LocalDateTime.now();
            }

            // Generate channelId for private chat (sorted user IDs)
            long id1 = Math.min(fromUserId != null ? fromUserId : 0, toUserId != null ? toUserId : 0);
            long id2 = Math.max(fromUserId != null ? fromUserId : 0, toUserId != null ? toUserId : 0);
            String channelId = "private_" + id1 + "_" + id2;

            ChatMessage cm = new ChatMessage(id, content, sender, channelId, ChatType.PRIVATE);
            cm.setMessageType(mt);
            cm.setStickerId(stickerId);
            cm.setStickerPath(stickerPath);
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

    /**
     * Fetch all available stickers
     * GET /api/stickers
     * 
     * @return List of Sticker
     */
    public static List<Sticker> fetchStickers() {
        try {
            String json = ApiClient.getAuth("/api/stickers");
            List<Map<String, Object>> stickers = MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>(){});
            
            return stickers.stream()
                    .map(ChatApi::mapSticker)
                    .filter(sticker -> sticker != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to fetch stickers: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Map StickerResponse to Sticker model
     */
    private static Sticker mapSticker(Map<String, Object> m) {
        try {
            Long id = null;
            Object idObj = m.get("id");
            if (idObj instanceof Number) {
                id = ((Number) idObj).longValue();
            }

            String stickerPath = String.valueOf(m.getOrDefault("stickerPath", ""));

            return new Sticker(id, stickerPath);
        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to parse sticker: " + e.getMessage());
            return null;
        }
    }
}
