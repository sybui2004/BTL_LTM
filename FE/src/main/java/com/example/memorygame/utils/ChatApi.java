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
            List<Map<String, Object>> contentList = MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>(){});

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
     * @param otherUserId Other user ID
     * @return List of ChatMessage for private channel
     */
    public static List<ChatMessage> fetchPrivateHistory(long otherUserId) {
        return fetchPrivateHistory(otherUserId, 0, 100);
    }

    /**
     * Fetch private chat history with pagination
     * 
     * @param otherUserId Other user ID
     * @param page Page number (0-based)
     * @param size Messages per page (max 100)
     * @return List of ChatMessage for private channel
     */
    public static List<ChatMessage> fetchPrivateHistory(long otherUserId, int page, int size) {
        try {
            String json = ApiClient.getAuth("/api/chat/private/" + otherUserId + "?page=" + page + "&size=" + size);
            
            Map<String, Object> pageData = MAPPER.readValue(json, new TypeReference<Map<String, Object>>(){});
            Object contentObj = pageData.get("content");
            if (contentObj == null) return Collections.emptyList();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> contentList = (List<Map<String, Object>>) contentObj;

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
     * @param otherUserId Other user ID
     * @return List of ChatMessage for private channel (all messages)
     */
    public static List<ChatMessage> fetchAllPrivateHistory(long otherUserId) {
        List<ChatMessage> allMessages = new ArrayList<>();
        int page = 0;
        int size = 100;
        
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
                
                Object totalPagesObj = pageData.get("totalPages");
                Object totalElementsObj = pageData.get("totalElements");
                boolean hasMore = false;
                
                if (totalPagesObj instanceof Number) {
                    int totalPages = ((Number) totalPagesObj).intValue();
                    hasMore = (page + 1) < totalPages;
                } else if (totalElementsObj instanceof Number) {
                    hasMore = pageMessages.size() >= size;
                } else {
                    hasMore = pageMessages.size() >= size;
                }
                
                if (!hasMore) break;
                page++;
            }
            
            System.out.println("[ChatApi] Fetched " + allMessages.size() + " messages from history");
            return allMessages;
        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to fetch all private history: " + e.getMessage());
            e.printStackTrace();
            return allMessages;
        }
    }

    /**
     * Fetch conversation list (users with last message)
     * GET /api/chat/private/conversations/{userId}
     * 
     * @param currentUserId Current user ID
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
     * @param currentUserId Current user ID
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
            sender.username = sender.displayName;
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

            LocalDateTime ts = null;
            Object createdAtObj = m.get("createdAt");
            
            if (createdAtObj instanceof Number) {
                ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) createdAtObj).longValue()), ZoneId.systemDefault());
            } else if (createdAtObj instanceof Map) {
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
                String timeStr = (String) createdAtObj;
                try {
                    long epochMillis = Long.parseLong(timeStr);
                    ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
                } catch (NumberFormatException e) {
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

            UserSummary sender = new UserSummary();
            if (fromUserId != null) {
                sender.id = fromUserId;
                try {
                    UserSummary fullSender = com.example.memorygame.utils.UserApi.getUserById(fromUserId);
                    if (fullSender != null) {
                        sender = fullSender;
                    } else {
                        sender.username = "User#" + fromUserId;
                    }
                } catch (Exception e) {
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

            // Timestamp parsing
            LocalDateTime ts = null;
            Object createdAtObj = m.get("createdAt");
            
            if (createdAtObj instanceof Number) {
                ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(((Number) createdAtObj).longValue()), ZoneId.systemDefault());
            } else if (createdAtObj instanceof Map) {
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
                String timeStr = (String) createdAtObj;
                try {
                    long epochMillis = Long.parseLong(timeStr);
                    ts = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
                } catch (NumberFormatException e) {
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
            
            String lastMsgTypeStr = String.valueOf(m.getOrDefault("lastMessageType", "TEXT"));
            try {
                conv.lastMessageType = MessageType.valueOf(lastMsgTypeStr);
            } catch (Exception ignored) {
                conv.lastMessageType = MessageType.TEXT;
            }

            LocalDateTime lastMsgTime = null;
            Object lastMsgTimeObj = m.get("lastMessageTime");
            if (lastMsgTimeObj instanceof Number) {
                long epochMillis = ((Number) lastMsgTimeObj).longValue();
                lastMsgTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault());
            } else if (lastMsgTimeObj instanceof String) {
                String timeStr = (String) lastMsgTimeObj;
                try {
                    long v = Long.parseLong(timeStr);
                    lastMsgTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(v), ZoneId.systemDefault());
                } catch (NumberFormatException e) {
                    try {
                        if (timeStr.endsWith("Z")) {
                            String withoutZ = timeStr.substring(0, timeStr.length() - 1);
                            lastMsgTime = LocalDateTime.parse(withoutZ);
                        } else {
                            lastMsgTime = LocalDateTime.parse(timeStr);
                        }
                    } catch (Exception e2) {
                        try {
                            Instant instant = Instant.parse(timeStr);
                            lastMsgTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                        } catch (Exception e3) {
                            System.err.println("[ChatApi] Failed to parse lastMessageTime: " + timeStr);
                        }
                    }
                }
            } else if (lastMsgTimeObj != null) {
                System.err.println("[ChatApi] Unknown lastMessageTime type: " + lastMsgTimeObj.getClass().getName());
            }
            
            conv.lastMessageTime = lastMsgTime;
            
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
     * Fetch stickers by type
     * GET /api/stickers?type={type}
     * 
     * @param type Sticker type ("NORMAL" or "MATCH")
     * @return List of Sticker filtered by type
     */
    public static List<Sticker> fetchStickersByType(String type) {
        try {
            String url = "/api/stickers?type=" + (type != null ? type.toUpperCase() : "NORMAL");
            String json = ApiClient.getAuth(url);
            List<Map<String, Object>> stickers = MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>(){});
            
            return stickers.stream()
                    .map(ChatApi::mapSticker)
                    .filter(sticker -> sticker != null)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to fetch stickers by type: " + e.getMessage());
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
            String type = String.valueOf(m.getOrDefault("type", "NORMAL"));

            return new Sticker(id, stickerPath, type);
        } catch (Exception e) {
            System.err.println("[ChatApi] Failed to parse sticker: " + e.getMessage());
            return null;
        }
    }
}
