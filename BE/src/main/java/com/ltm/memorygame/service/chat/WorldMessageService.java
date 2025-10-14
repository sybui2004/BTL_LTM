package com.ltm.memorygame.service.chat;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    //Lưu tin nhắn toàn cầu 
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

        // realtime
        return response;
    }

    //Lấy danh sách 100 tin nhắn gần nhất (mặc định)
    @Transactional(readOnly = true)
    public Page<WorldMessageResponse> getRecentMessages(int page, int size) {
        Page<WorldMessage> messages
                = worldMessageRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size));
        return messages.map(MessageMapper::toWorldMessageResponse);
    }

}
