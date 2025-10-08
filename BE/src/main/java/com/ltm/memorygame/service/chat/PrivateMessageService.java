package com.ltm.memorygame.service.chat;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.ltm.memorygame.dao.chat.PrivateMessageRepository;
import com.ltm.memorygame.dto.chat.response.PrivateMessageResponse;
import com.ltm.memorygame.mapper.MessageMapper;
import com.ltm.memorygame.model.chat.PrivateMessage;
import com.ltm.memorygame.model.game.Match;
import com.ltm.memorygame.model.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PrivateMessageService {

    private final PrivateMessageRepository privateMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi tin nhắn riêng giữa hai người dùng, lưu vào DB và phát realtime cho người nhận.
     */
    public PrivateMessageResponse sendPrivateMessage(User sender, User receiver, String content, Match match) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        PrivateMessage message = new PrivateMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setMatch(match);
        message.setContent(content.trim());
        message.setCreatedAt(java.util.Date.from(Instant.now()));

        PrivateMessage saved = privateMessageRepository.save(message);
        PrivateMessageResponse response = MessageMapper.toPrivateMessageResponse(saved);

        // gửi realtime đến người nhận qua /user/queue/private
        messagingTemplate.convertAndSendToUser(
                receiver.getId().toString(),
                "/queue/private",
                response
        );
        messagingTemplate.convertAndSendToUser(
                sender.getId().toString(),
                "/queue/private",
                response
        );

        return response;
    }

    //Lấy lịch sử hội thoại giữa hai người dùng, phân trang theo thời gian tăng dần.
    public Page<PrivateMessageResponse> getConversation(Long userAId, Long userBId, int page, int size) {
        Page<PrivateMessage> messages = privateMessageRepository.findConversation(userAId, userBId, PageRequest.of(page, size));
        return messages.map(MessageMapper::toPrivateMessageResponse);
    }
}
