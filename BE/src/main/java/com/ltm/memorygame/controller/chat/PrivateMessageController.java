package com.ltm.memorygame.controller.chat;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ltm.memorygame.dto.chat.response.ConversationPreviewDTO;
import com.ltm.memorygame.dto.chat.response.PrivateMessageResponse;
import com.ltm.memorygame.service.chat.PrivateMessageService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/private")
public class PrivateMessageController {

    private final PrivateMessageService privateMessageService;
    
    // lấy tất cả message của 1 đoạn chat
    @GetMapping("/{otherUserId}")  
    public Page<PrivateMessageResponse> getPrivateMessageHistory(
            @RequestHeader("userId") Long userId,
            @PathVariable("otherUserId") Long otherUserId,
            @RequestHeader(name = "page", required = false) Integer page,
            @RequestHeader(name = "size", required = false) Integer size) {

        int p = (page == null || page < 0) ? 0 : page;
        int s = (size == null || size <= 0 || size > 100) ? 20 : size;
        return privateMessageService.getPrivateMessageHistory(userId, otherUserId, p, s);
    }
    
    // lấy danh sách người nhắn tin
    @GetMapping("/conversations/{userId}")
    public ResponseEntity<List<ConversationPreviewDTO>> listConversations(@PathVariable Long userId) {
        return ResponseEntity.ok(privateMessageService.getLatestConversations(userId));
    }
}
