package com.ltm.memorygame.controller.chat;

import java.security.Principal;
import java.util.List;

import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import com.ltm.memorygame.dao.user.UserRepository;
import com.ltm.memorygame.dto.chat.request.WorldMessageRequest;
import com.ltm.memorygame.dto.chat.response.WorldMessageResponse;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.service.chat.WorldMessageService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class WorldMessageController {

    private final WorldMessageService worldMessageService;
    private final UserRepository userRepository;

//Client gửi STOMP đến /app/world
    @MessageMapping("/world")
    public void sendWorldMessage(
            @Valid @Payload WorldMessageRequest request,
            @Header(name = "X-User-Id", required = false) Long headerUserId,
            Principal principal
    ) {
        User sender = resolveSender(headerUserId, principal);

        worldMessageService.postWorldMessage(sender, request.getContent());
    }

    //Lấy lịch sử 100 tin nhắn của kênh thế giới
    @MessageMapping("/world.history")
    @SendToUser("/queue/world.history")
    public List<WorldMessageResponse> getWorldHistory(
            Principal principal,
            @Header(name = "limit", required = false) Integer limit
    ) {
        int l = (limit == null || limit <= 0 || limit > 100) ? 100 : limit;
        return worldMessageService.getRecentMessages(l);
    }

// helper
    private User resolveSender(Long headerUserId, Principal principal) {
        if (headerUserId != null) {
            return userRepository.findById(headerUserId)
                    .orElseThrow(() -> new IllegalArgumentException("Sender not found (id=" + headerUserId + ")"));
        }
        if (principal == null || principal.getName() == null) {
            throw new IllegalStateException("Missing user identity (no X-User-Id and no Principal)");
        }
        return userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("Sender not found (username=" + principal.getName() + ")"));
    }
}
