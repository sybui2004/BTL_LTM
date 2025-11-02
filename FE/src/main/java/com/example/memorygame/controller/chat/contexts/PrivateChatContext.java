package com.example.memorygame.controller.chat.contexts;

import java.util.Arrays;
import java.util.List;

import com.example.memorygame.controller.chat.ChatContext;
import com.example.memorygame.model.chat.ChatType;
import com.example.memorygame.model.user.UserSummary;

/**
 * Context cho Private Chat - chat 1-1 giữa users
 * 
 * Đặc điểm:
 * - Chat riêng tư giữa 2 users
 * - Lưu trữ trong DB (có history)
 * - Hiển thị online/offline status
 * - Không phụ thuộc game state
 * - Có conversation list
 * 
 * Khi khởi tạo, context sẽ tự động fetch lịch sử tin nhắn giữa 2 users từ server.
 */
public class PrivateChatContext implements ChatContext {
    private final UserSummary currentUser;
    private final UserSummary otherUser;
    private final String conversationId;
    private List<com.example.memorygame.model.chat.ChatMessage> historyMessages;
    
    public PrivateChatContext(UserSummary currentUser, UserSummary otherUser) {
        this.currentUser = currentUser;
        this.otherUser = otherUser;
        // Generate conversation ID từ 2 user IDs (sorted để đảm bảo unique)
        long id1 = Math.min(currentUser.id, otherUser.id);
        long id2 = Math.max(currentUser.id, otherUser.id);
        this.conversationId = "private_" + id1 + "_" + id2;
        // Fetch lịch sử tin nhắn ngay khi khởi tạo context
        loadHistory();
    }
    
    /**
     * Load lịch sử tin nhắn private chat từ server
     */
    private void loadHistory() {
        try {
            this.historyMessages = com.example.memorygame.utils.ChatApi.fetchPrivateHistory(otherUser.id);
            System.out.println("[PrivateChatContext] Loaded " + historyMessages.size() + " messages from history with " + otherUser.username);
        } catch (Exception e) {
            System.err.println("[PrivateChatContext] Failed to load history: " + e.getMessage());
            this.historyMessages = java.util.Collections.emptyList();
        }
    }
    
    /**
     * Lấy danh sách tin nhắn lịch sử (để hiển thị khi khởi tạo ChatComponent)
     */
    public List<com.example.memorygame.model.chat.ChatMessage> getHistoryMessages() {
        return historyMessages != null ? historyMessages : java.util.Collections.emptyList();
    }
    
    @Override
    public String getChannelId() {
        return conversationId;
    }
    
    @Override
    public ChatType getType() {
        return ChatType.PRIVATE;
    }
    
    @Override
    public UserSummary getCurrentUser() {
        return currentUser;
    }
    
    @Override
    public List<UserSummary> getParticipants() {
        return Arrays.asList(currentUser, otherUser);
    }
    
    @Override
    public boolean canSendMessage() {
        // Luôn cho phép gửi tin (kể cả khi user offline)
        return currentUser != null && otherUser != null;
    }
    
    @Override
    public String getTitle() {
        String name = otherUser.displayName != null && !otherUser.displayName.isBlank() 
            ? otherUser.displayName 
            : otherUser.username;
        return "Chat with " + name;
    }
    
    @Override
    public boolean showUserStatus() {
        // Hiển thị online/offline status
        return true;
    }
    
    @Override
    public boolean showEmoji() {
        return true;
    }
    
    @Override
    public boolean showTimestamp() {
        return true; // Private chat cần timestamp để track conversation
    }
    
    // Getters
    public UserSummary getOtherUser() {
        return otherUser;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    /**
     * Check xem user kia có đang online không
     */
    public boolean isOtherUserOnline() {
        return otherUser != null && "online".equalsIgnoreCase(otherUser.status);
    }
}
