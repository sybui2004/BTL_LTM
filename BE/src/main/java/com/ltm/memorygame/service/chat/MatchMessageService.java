package com.ltm.memorygame.service.chat;

import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    // Bộ đệm tin nhắn tạm: mỗi phòng có tối đa 200 tin
    public MatchMessageDTO sendMatchMessage(String roomId, Long fromUserId, String content, MessageType type,
            Long stickerId) {

        StickerResponse stickerResponse = null;

        if (stickerId != null) {
            var sticker = stickerRepository.findById(stickerId)
                    .orElseThrow(() -> new IllegalArgumentException("Sticker not found: " + stickerId));
            stickerResponse = StickerMapper.toResponse(sticker);
        }

        // 1. Tạo DTO chung
        MatchMessageDTO message = MessageMapper.toMatchMessageDTO(roomId, fromUserId, content, type, stickerResponse);

        // 2. Tra cứu Room Session (Giả định RoomSessionManager được inject)
        RoomSession session = RoomSessionManager.getRoom(roomId);
        if (session == null) {
            throw new NoSuchElementException("Room session not found for ID: " + roomId);
        }

        // 3. Logic LƯU TRỮ CÓ ĐIỀU KIỆN
        // Chỉ lưu tin nhắn văn bản vào Deque (như thiết kế ban đầu)
        if (type == MessageType.TEXT) {
            session.addMessage(message);
        }

        return message;
    }

    // Lấy snapshot các tin nhắn hiện có trong room phòng khi break giữa chừng
    @Transactional
    public List<MatchMessageDTO> getMatchMessageHistory(String roomId) {
        RoomSession session = RoomSessionManager.getRoom(roomId);
        return session.getHistory();
    }

    // Xóa lịch sử tạm sau khi trận kết thúc.
    public void clearMatch(String roomId) {
        RoomSessionManager.removeRoom(roomId); // Xóa toàn bộ Session (và Deque bên trong)
    }
}
