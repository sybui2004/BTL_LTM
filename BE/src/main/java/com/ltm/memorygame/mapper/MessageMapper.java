package com.ltm.memorygame.mapper;

import java.time.Instant;
import java.time.ZoneOffset;

import com.ltm.memorygame.dao.chat.ConversationPreviewProjection;
import com.ltm.memorygame.dto.chat.response.ConversationPreviewDTO;
import com.ltm.memorygame.dto.chat.response.MatchMessageDTO;
import com.ltm.memorygame.dto.chat.response.PrivateMessageResponse;
import com.ltm.memorygame.dto.chat.response.StickerResponse;
import com.ltm.memorygame.dto.chat.response.WorldMessageResponse;
import com.ltm.memorygame.model.chat.PrivateMessage;
import com.ltm.memorygame.model.chat.Sticker;
import com.ltm.memorygame.model.chat.WorldMessage;
import com.ltm.memorygame.model.enums.MessageType;
import com.ltm.memorygame.model.user.User;

public class MessageMapper {

    private MessageMapper() {
        // prevent instantiation
    }

    // WorldMessage
    public static WorldMessageResponse toWorldMessageResponse(WorldMessage entity) {
        if (entity == null) return null;

        User sender = entity.getSender();
        Sticker sticker = entity.getSticker();

        return WorldMessageResponse.builder()
                .id(entity.getId())
                .senderId(sender != null ? sender.getId() : null)
                .senderName(sender != null ? sender.getDisplayName() : null)
                .content(entity.getContent())
                .avatarUrl(entity.getSender().getAvatarUrl())
                .messageType(entity.getMessageType())
                .sticker(sticker != null ? StickerMapper.toResponse(sticker) : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant() : null)
                .build();
    }

    // PrivateMessage
    public static PrivateMessageResponse toPrivateMessageResponse(PrivateMessage entity) {
        if (entity == null) return null;
        Sticker sticker = entity.getSticker();

        return PrivateMessageResponse.builder()
                .id(entity.getId())
                .fromUserId(entity.getSender() != null ? entity.getSender().getId() : null)
                .toUserId(entity.getReceiver() != null ? entity.getReceiver().getId() : null)
                .matchId(entity.getMatch() != null ? entity.getMatch().getId() : null)
                .content(entity.getContent())
                .avatarUrl(entity.getSender().getAvatarUrl())
                .messageType(entity.getMessageType())
                .sticker(sticker != null ? StickerMapper.toResponse(sticker) : null)
                .createdAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toInstant() : null)
                .build();
    }

    // MatchMessage
    public static MatchMessageDTO toMatchMessageDTO(
            String roomId,
            Long fromUserId,
            String content,
            MessageType type,
            StickerResponse sticker
    ) {
        return MatchMessageDTO.builder()
                .roomId(roomId)
                .fromUserId(fromUserId)
                .content(type == MessageType.TEXT ? content : null)
                .sticker(type == MessageType.STICKER ? sticker : null)
                .messageType(type)
                .createdAt(Instant.now())
                .build();
    }

    // danh sách người từng nhắn và tin nhắn cuối cùng
    public static ConversationPreviewDTO toConversationPreviewDTO(
            ConversationPreviewProjection p, Long currentUserId) {

        boolean fromSelf = p.getSenderId() != null && p.getSenderId().equals(currentUserId);

        return ConversationPreviewDTO.builder()
            .otherUserId(p.getOtherUserId())
            .otherUsername(p.getOtherUsername())
            .otherDisplayName(p.getOtherDisplayName())
            .otherAvatarUrl(p.getOtherAvatarUrl())
            .lastMessageId(p.getLastMessageId())
            .lastMessageText(p.getLastMessageText())
            .lastMessageType(p.getLastMessageType())
            .lastStickerId(p.getLastStickerId())
            .lastMessageTime(p.getLastMessageTime() == null ? null : p.getLastMessageTime().toInstant(ZoneOffset.UTC))
            .lastMessageFromSelf(fromSelf)
            .build();
    }

}
