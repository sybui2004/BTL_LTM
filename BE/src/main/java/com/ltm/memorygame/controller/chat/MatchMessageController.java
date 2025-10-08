package com.ltm.memorygame.controller.chat;

import java.security.Principal;
import java.util.List;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import com.ltm.memorygame.dto.chat.request.MatchMessageRequest;
import com.ltm.memorygame.dto.chat.response.MatchMessageDto;
import com.ltm.memorygame.service.chat.MatchMessageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class MatchMessageController {

    private final MatchMessageService matchMessageService;

    // Client gửi tin nhắn vào: /app/room.send.{roomId}

    @MessageMapping("/room.send.{roomId}")
    public void handleRoomMessage(@DestinationVariable String roomId,
                                  @Valid @Payload MatchMessageRequest request,
                                  Principal principal) {
        Long fromUserId = Long.valueOf(principal.getName());
        matchMessageService.sendMatchMessage(roomId, fromUserId, request.getContent());
    }

    
    // Khi client subscribe /topic/room.{roomId}, server trả snapshot tin nhắn hiện có
    
    @SubscribeMapping("/topic/room.{roomId}")
    public List<MatchMessageDto> getRoomHistory(@DestinationVariable String roomId) {
        return matchMessageService.getMatchHistory(roomId);
    }
}
