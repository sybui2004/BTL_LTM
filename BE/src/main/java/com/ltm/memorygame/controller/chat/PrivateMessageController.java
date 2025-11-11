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
        @RequestHeader(value = "userId", required = false) Long userIdHeader,
        @PathVariable Long otherUserId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
    // If userId not in header, extract from JWT token (handled by security context)
    // For now, require userId in header or implement JWT extraction
    if (userIdHeader == null) {
        throw new IllegalArgumentException("Missing userId in request header");
    }
    return privateMessageService.getPrivateMessageHistory(userIdHeader, otherUserId, page, size);
}
    
    // lấy danh sách người nhắn tin
    @GetMapping("/conversations/{userId}")
    public ResponseEntity<List<ConversationPreviewDTO>> listConversations(@PathVariable Long userId) {
        return ResponseEntity.ok(privateMessageService.getLatestConversations(userId));
    }

    // Lấy danh sách tất cả bạn bè kèm thông tin conversation (nếu có)
    // Trả về danh sách bạn bè, merge với conversation preview nếu đã từng nhắn tin
    @GetMapping("/friends-with-conversations/{userId}")
    public ResponseEntity<List<ConversationPreviewDTO>> getFriendsWithConversations(@PathVariable Long userId) {
        return ResponseEntity.ok(privateMessageService.getFriendsWithConversations(userId));
    }
}
