package com.ltm.memorygame.service.chat;

import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;

import com.ltm.memorygame.dao.chat.StickerRepository;
import com.ltm.memorygame.dto.chat.response.MatchMessageDTO;
import com.ltm.memorygame.dto.chat.response.StickerResponse;
import com.ltm.memorygame.mapper.MessageMapper;
import com.ltm.memorygame.mapper.StickerMapper;
import com.ltm.memorygame.model.enums.MessageType;
import com.ltm.memorygame.tcp.RoomSession;
import com.ltm.memorygame.tcp.RoomSessionManager;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchMessageService {
    private final StickerRepository stickerRepository;

    /**
     * Gửi tin nhắn match và lưu vào bộ đệm RAM
     * Chỉ lưu tin nhắn văn bản vào Deque (tối đa 200 tin)
     */
    public MatchMessageDTO sendMatchMessage(String roomId, Long fromUserId, String content, MessageType type,
            Long stickerId) {

        StickerResponse stickerResponse = null;

        if (stickerId != null) {
            var sticker = stickerRepository.findById(stickerId)
                    .orElseThrow(() -> new IllegalArgumentException("Sticker not found: " + stickerId));
            stickerResponse = StickerMapper.toResponse(sticker);
        }

        MatchMessageDTO message = MessageMapper.toMatchMessageDTO(roomId, fromUserId, content, type, stickerResponse);

        RoomSession session = RoomSessionManager.getRoom(roomId);
        if (session == null) {
            throw new NoSuchElementException("Room session not found for ID: " + roomId);
        }

        // Chỉ lưu tin nhắn văn bản vào Deque
        if (type == MessageType.TEXT) {
            session.addMessage(message);
        }

        return message;
    }
}
