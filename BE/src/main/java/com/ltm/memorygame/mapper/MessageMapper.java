package com.ltm.memorygame.mapper;

import com.ltm.memorygame.dto.chat.response.MatchMessageDto;
import com.ltm.memorygame.dto.chat.response.PrivateMessageResponse;
import com.ltm.memorygame.dto.chat.response.WorldMessageResponse;
import com.ltm.memorygame.model.chat.PrivateMessage;
import com.ltm.memorygame.model.chat.WorldMessage;
import com.ltm.memorygame.model.user.User;

public class MessageMapper {

    private MessageMapper() {
        // prevent instantiation
    }

    // WorldMessage
    public static WorldMessageResponse toWorldMessageResponse(WorldMessage entity) {
        if (entity == null) return null;

        User sender = entity.getSender();

        return WorldMessageResponse.builder()
                .id(entity.getId())
                .senderId(sender != null ? sender.getId() : null)
                .senderName(sender != null ? getDisplayName(sender) : null)
                .content(entity.getContent())
                .avatarUrl(entity.getSender().getAvatarUrl())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant() : null)
                .build();
    }

    // PrivateMessage
    public static PrivateMessageResponse toPrivateMessageResponse(PrivateMessage entity) {
        if (entity == null) return null;

        return PrivateMessageResponse.builder()
                .id(entity.getId())
                .fromUserId(entity.getSender() != null ? entity.getSender().getId() : null)
                .toUserId(entity.getReceiver() != null ? entity.getReceiver().getId() : null)
                .matchId(entity.getMatch() != null ? entity.getMatch().getId() : null)
                .content(entity.getContent())
                .avatarUrl(entity.getSender().getAvatarUrl())
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant() : null)
                .build();
    }

    // MatchMessage
    public static MatchMessageDto toMatchMessageDto(String roomId, Long fromUserId, String content) {
        return MatchMessageDto.builder()
                .roomId(roomId)
                .fromUserId(fromUserId)
                .content(content)
                .createdAt(java.time.Instant.now())
                .build();
    }

    private static String getDisplayName(User user) {
        if (user.getDisplayName() != null && !user.getDisplayName().isBlank()) {
            return user.getDisplayName();
        }
        return user.getUsername();
    }
}
