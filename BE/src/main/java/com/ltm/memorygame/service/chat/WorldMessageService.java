package com.ltm.memorygame.service.chat;

import java.time.Instant;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltm.memorygame.dao.chat.StickerRepository;
import com.ltm.memorygame.dao.chat.WorldMessageRepository;
import com.ltm.memorygame.dto.chat.response.WorldMessageResponse;
import com.ltm.memorygame.mapper.MessageMapper;
import com.ltm.memorygame.model.chat.Sticker;
import com.ltm.memorygame.model.chat.WorldMessage;
import com.ltm.memorygame.model.enums.MessageType;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.service.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorldMessageService {

    private final WorldMessageRepository worldMessageRepository;
    private final UserService userService;
    private final StickerRepository stickerRepository;

    // Lưu tin nhắn toàn cầu
    public WorldMessageResponse postWorldMessage(Long senderId, String content, MessageType type,
            Long stickerId) {
        // Validation theo loại message
        if (type == MessageType.TEXT) {
            if (content == null || content.isBlank()) {
                throw new IllegalArgumentException("Message content cannot be empty for TEXT messages");
            }
        } else if (type == MessageType.STICKER) {
            // Cho phép content rỗng với STICKER, nhưng cần stickerId hợp lệ
            if (stickerId == null) {
                throw new IllegalArgumentException("Sticker id is required for STICKER messages");
            }
        }

        Sticker sticker = null;
        if (stickerId != null) {
            sticker = stickerRepository.findById(stickerId)
                    .orElseThrow(() -> new IllegalArgumentException("Sticker not found: " + stickerId));
        }

        User sender = userService.getEntityById(senderId);

        WorldMessage message = new WorldMessage();
        message.setSender(sender);
        // DB column `content` is NOT NULL, đảm bảo không null
        String persistedContent = (type == MessageType.TEXT) ? content.trim() : "";
        message.setContent(persistedContent);
        message.setCreatedAt(java.util.Date.from(Instant.now()));
        message.setMessageType(type);
        message.setSticker(sticker);

        WorldMessage saved = worldMessageRepository.save(message);
        WorldMessageResponse response = MessageMapper.toWorldMessageResponse(saved);

        // realtime
        return response;
    }

    // Lấy danh sách 100 tin nhắn gần nhất
    // Query lấy DESC (mới nhất trước) nhưng đảo ngược để trả về ASC (cũ nhất trước, mới nhất sau)
    // để phù hợp với cách frontend hiển thị (scroll từ trên xuống, tin mới nhất ở dưới)
    @Transactional(readOnly = true)
    public List<WorldMessageResponse> getRecentMessages() {
        List<WorldMessage> messages = worldMessageRepository.findTop100ByOrderByCreatedAtDesc();
        // Đảo ngược list để trả về thứ tự ASC (oldest first, newest last)
        java.util.Collections.reverse(messages);
        return messages.stream()
                .map(MessageMapper::toWorldMessageResponse)
                .collect(java.util.stream.Collectors.toList());
    }

}
