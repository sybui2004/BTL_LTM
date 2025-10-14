package com.ltm.memorygame.service.chat;

import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ltm.memorygame.dto.chat.response.MatchMessageDTO;
import com.ltm.memorygame.dto.chat.response.StickerResponse;
import com.ltm.memorygame.mapper.MessageMapper;
import com.ltm.memorygame.model.enums.MessageType;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MatchMessageService {


    // Bộ đệm tin nhắn tạm: mỗi phòng có tối đa 200 tin
    private static final int MAX_ROOM_MESSAGES = 200;
    private final Map<String, Deque<MatchMessageDTO>> roomBuffers = new ConcurrentHashMap<>();


    // gửi text (được lưu lại trong deque)
    public MatchMessageDTO sendMatchText(String roomId, Long fromUserId, String content) {
        
        MatchMessageDTO message = MessageMapper.toMatchMessageDTO(roomId, fromUserId, content.trim(), MessageType.TEXT, null);
        Deque<MatchMessageDTO> buffer = roomBuffers.computeIfAbsent(roomId, k -> new ConcurrentLinkedDeque<>());

        if (buffer.size() >= MAX_ROOM_MESSAGES) {
            buffer.pollFirst();
        }
        buffer.addLast(message);

        // Gửi realtime
        return message;
    }

    // gửi sticker (không lưu vào deque)
     public MatchMessageDTO sendSticker(String roomId, Long fromUserId, StickerResponse sticker) {
        //realtime
        return MessageMapper.toMatchMessageDTO(
                roomId, fromUserId, null, MessageType.STICKER, sticker
        );
    }
    
    //Lấy snapshot các tin nhắn hiện có trong room phòng khi break giữa chừng
    @Transactional
    public List<MatchMessageDTO> getMatchMessageHistory(String roomId) {
        Deque<MatchMessageDTO> buffer = roomBuffers.get(roomId);
        if (buffer == null) return Collections.emptyList();
        return buffer.stream()
                .sorted(Comparator.comparing(MatchMessageDTO::getCreatedAt))
                .collect(Collectors.toList());
    }

    // Xóa lịch sử tạm sau khi trận kết thúc.
    public void clearMatch(String roomId) {
        roomBuffers.remove(roomId);
    }
}
