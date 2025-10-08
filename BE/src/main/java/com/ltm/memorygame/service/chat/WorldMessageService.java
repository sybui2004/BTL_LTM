package com.ltm.memorygame.service.chat;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.ltm.memorygame.dao.chat.WorldMessageRepository;
import com.ltm.memorygame.dto.chat.response.WorldMessageResponse;
import com.ltm.memorygame.mapper.MessageMapper;
import com.ltm.memorygame.model.chat.WorldMessage;
import com.ltm.memorygame.model.user.User;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WorldMessageService {

    private final WorldMessageRepository worldMessageRepository;
    private final SimpMessagingTemplate messagingTemplate;

    //Lưu tin nhắn toàn cầu và broadcast ra /topic/global
    public WorldMessageResponse postWorldMessage(User sender, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }

        WorldMessage message = new WorldMessage();
        message.setSender(sender);
        message.setContent(content.trim());
        message.setCreatedAt(java.util.Date.from(Instant.now()));

        WorldMessage saved = worldMessageRepository.save(message);
        WorldMessageResponse response = MessageMapper.toWorldMessageResponse(saved);

        // phát realtime tới tất cả client
        messagingTemplate.convertAndSend("/topic/global", response);
        return response;
    }


    //Lấy danh sách 100 tin nhắn gần nhất (mặc định)

    public List<WorldMessageResponse> getRecentMessages(int limit) {
        List<WorldMessage> messages = worldMessageRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit));
        return messages.stream()
                .map(MessageMapper::toWorldMessageResponse)
                .collect(Collectors.toList());
    }
}
