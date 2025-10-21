package com.example.memorygame.utils;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.example.memorygame.model.chat.ConversationPreview;
import com.example.memorygame.model.chat.PrivateMessage;
import com.example.memorygame.model.chat.Sticker;
import com.example.memorygame.model.enums.MessageType;
import com.example.memorygame.model.user.UserSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ChatApi {
    private static final String BASE_URL = "/api/chat";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static class StickerResponseApiDTO {
        public Long id;
        public String sticker_path;
    }

    private static class PrivateMessageResponseApiDTO {
        public Long id;
        public Long fromUserId;
        public Long toUserId;
        public Long matchId;
        public String content;
        public String messageType;
        public StickerResponseApiDTO sticker;
        public Instant createdAt;
    }

    // DTO cho Page<PrivateMessageResponse>
    private static class PageResponseApiDTO {
        public List<PrivateMessageResponseApiDTO> content;
        public int totalPages;
        // ... (các thuộc tính pagination khác)
    }

    private static class ConversationPreviewApiDTO {
        public UserSummary otherUser;
        public String lastMessageContent;
        public String messageType;
        public Instant createdAt;
    }

    public static List<ConversationPreview> getLatestConversations(Long userId) {
        try {
            String url = BASE_URL + "private/conversations/" + userId;
            String json = ApiClient.get(url);

            List<ConversationPreviewApiDTO> dtoList = MAPPER.readValue(json,
                    new TypeReference<List<ConversationPreviewApiDTO>>() {
                    });
            return dtoList.stream().map(dto -> new ConversationPreview(
                    dto.otherUser,
                    dto.lastMessageContent,
                    MessageType.valueOf(dto.messageType), // Chuyển đổi String sang FE Enum
                    dto.createdAt)).collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error fetching latest conversations: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static List<PrivateMessage> getPrivateMessageHistory(Long otherUserId, int page, int size) {
        try {
            String url = BASE_URL + "private/" + otherUserId + "?page=" + page + "&size=" + size;
            String json = ApiClient.get(url);

            PageResponseApiDTO pageDto = MAPPER.readValue(json, PageResponseApiDTO.class);

            if (pageDto.content == null) {
                return Collections.emptyList();
            }

            return pageDto.content.stream().map(dto -> {
                Sticker sticker = null;
                if (dto.sticker != null) {
                    sticker = new Sticker(dto.sticker.id, dto.sticker.sticker_path);
                }
                return new PrivateMessage(
                        dto.id,
                        dto.fromUserId,
                        dto.toUserId,
                        dto.matchId,
                        dto.content,
                        MessageType.valueOf(dto.messageType),
                        sticker,
                        dto.createdAt);
            }).collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Error fetching private message history: " + e.getMessage());
            return Collections.emptyList();
        }
    }

}