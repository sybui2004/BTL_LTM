package com.ltm.memorygame.controller.chat;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    
@GetMapping("/{otherUserId}")
public Page<PrivateMessageResponse> getPrivateMessageHistory(
        @RequestHeader("userId") Long userId,
        @PathVariable Long otherUserId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    return privateMessageService.getPrivateMessageHistory(userId, otherUserId, page, size);
}
    
    // lấy danh sách người nhắn tin
    @GetMapping("/conversations/{userId}")
    public ResponseEntity<List<ConversationPreviewDTO>> listConversations(@PathVariable Long userId) {
        return ResponseEntity.ok(privateMessageService.getLatestConversations(userId));
    }
}
