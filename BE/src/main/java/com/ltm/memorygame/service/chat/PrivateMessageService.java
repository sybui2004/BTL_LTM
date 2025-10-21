package com.ltm.memorygame.service.chat;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltm.memorygame.dao.chat.PrivateMessageRepository;
import com.ltm.memorygame.dao.chat.StickerRepository;
import com.ltm.memorygame.dto.chat.response.ConversationPreviewDTO;
import com.ltm.memorygame.dto.chat.response.PrivateMessageResponse;
import com.ltm.memorygame.mapper.MessageMapper;
import com.ltm.memorygame.model.chat.PrivateMessage;
import com.ltm.memorygame.model.chat.Sticker;
import com.ltm.memorygame.model.enums.MessageType;
import com.ltm.memorygame.model.game.Match;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.service.user.UserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrivateMessageService {

    private final PrivateMessageRepository privateMessageRepository;
    private final UserService userService;
    private final StickerRepository stickerRepository;
    
    // Lưu tin nhắn riêng vào db
    public PrivateMessageResponse sendPrivateMessage(Long senderId, Long receiverId, String content, Match match, MessageType type,
            Long stickerId) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        Sticker sticker=null;
        if (stickerId != null) {
            sticker = stickerRepository.findById(stickerId)
                    .orElseThrow(() -> new IllegalArgumentException("Sticker not found: " + stickerId));
        }

        User sender = userService.getEntityById(senderId);

        User receiver = userService.getEntityById(receiverId);

        PrivateMessage message = new PrivateMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setMatch(match);
        message.setContent(content.trim());
        message.setCreatedAt(java.util.Date.from(Instant.now()));
        message.setMessageType(type);
        message.setSticker(sticker);


        PrivateMessage saved = privateMessageRepository.save(message);
        PrivateMessageResponse response = MessageMapper.toPrivateMessageResponse(saved);

        // realtime

        return response;
    }

    //Lấy lịch sử hội thoại giữa hai người dùng, phân trang theo thời gian tăng dần.
    @Transactional(readOnly = true)
    public Page<PrivateMessageResponse> getPrivateMessageHistory(Long userAId, Long userBId, int page, int size) {
        Page<PrivateMessage> messages = privateMessageRepository.findConversation(userAId, userBId, PageRequest.of(page, size));
        return messages.map(MessageMapper::toPrivateMessageResponse);
    }

    // Lấy danh sách user từng nhắn và tin nhắn cuối cùng
    @Transactional(readOnly = true)
    public List<ConversationPreviewDTO> getLatestConversations(Long userId) {
        return privateMessageRepository.findLatestConversations(userId)
                .stream()
                .map(p -> MessageMapper.toConversationPreviewDTO(p, userId))
                .collect(Collectors.toList());
    }
}
