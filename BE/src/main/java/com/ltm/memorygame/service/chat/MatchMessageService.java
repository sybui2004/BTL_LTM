package com.ltm.memorygame.service.chat;

import java.time.Instant;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import com.ltm.memorygame.dto.chat.response.MatchMessageDto;
import com.ltm.memorygame.mapper.MessageMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchMessageService {

    private final SimpMessagingTemplate messagingTemplate;

    // Bộ đệm tin nhắn tạm: mỗi phòng có tối đa 200 tin
    private static final int MAX_ROOM_MESSAGES = 200;
    private final Map<String, Deque<MatchMessageDto>> roomBuffers = new ConcurrentHashMap<>();


    // Gửi tin nhắn trong phòng và broadcast tới tất cả người trong room.
    public MatchMessageDto sendMatchMessage(String roomId, Long fromUserId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content cannot be empty");
        }
        
        MatchMessageDto message = MessageMapper.toMatchMessageDto(roomId, fromUserId, content.trim());
        Deque<MatchMessageDto> buffer = roomBuffers.computeIfAbsent(roomId, k -> new ConcurrentLinkedDeque<>());

        if (buffer.size() >= MAX_ROOM_MESSAGES) {
            buffer.pollFirst();
        }
        buffer.addLast(message);

        // Gửi realtime
        messagingTemplate.convertAndSend("/topic/room." + roomId, message);
        return message;
    }

    
    //Lấy snapshot các tin nhắn hiện có trong room phòng khi break giữa chừng
    public List<MatchMessageDto> getMatchHistory(String roomId) {
        Deque<MatchMessageDto> buffer = roomBuffers.get(roomId);
        if (buffer == null) return Collections.emptyList();
        return buffer.stream()
                .sorted(Comparator.comparing(MatchMessageDto::getCreatedAt))
                .collect(Collectors.toList());
    }

    /**
     * Xóa lịch sử tạm sau khi trận kết thúc.
     */
    public void clearMatch(String roomId) {
        roomBuffers.remove(roomId);
        messagingTemplate.convertAndSend("/topic/room." + roomId, 
                "Room " + roomId + " chat cleared at " + Instant.now());
    }
}
