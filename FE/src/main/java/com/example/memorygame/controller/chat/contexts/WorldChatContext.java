package com.example.memorygame.controller.chat.contexts;

import java.util.Collections;
import java.util.List;

import com.example.memorygame.controller.chat.ChatContext;
import com.example.memorygame.model.chat.ChatMessage;
import com.example.memorygame.model.chat.ChatType;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.ChatApi;

/**
 * Context cho World Chat - chat toàn server
 * 
 * Đặc điểm:
 * - Chat chung cho tất cả users online
 * - Lưu DB (BE xử lý), FE hiển thị messages gần đây (100 tin nhắn mặc định)
 * - Không phụ thuộc vào game state
 * - Có thể floating hoặc docked
 * 
 * Khi khởi tạo, context sẽ tự động fetch lịch sử 100 tin nhắn gần nhất từ server.
 */
public class WorldChatContext implements ChatContext {
    private final UserSummary currentUser;
    private List<ChatMessage> historyMessages;
    
    public WorldChatContext(UserSummary currentUser) {
        this.currentUser = currentUser;
        // Fetch lịch sử tin nhắn ngay khi khởi tạo context
        loadHistory();
    }
    
    /**
     * Load lịch sử 100 tin nhắn world chat gần nhất từ server
     */
    private void loadHistory() {
        try {
            this.historyMessages = ChatApi.fetchWorldHistory();
            System.out.println("[WorldChatContext] Loaded " + historyMessages.size() + " messages from history");
        } catch (Exception e) {
            System.err.println("[WorldChatContext] Failed to load history: " + e.getMessage());
            this.historyMessages = Collections.emptyList();
        }
    }
    
    /**
     * Lấy danh sách tin nhắn lịch sử (để hiển thị khi khởi tạo ChatComponent)
     */
    public List<ChatMessage> getHistoryMessages() {
        return historyMessages != null ? historyMessages : Collections.emptyList();
    }
    
    @Override
    public String getChannelId() {
        return "world";
    }
    
    @Override
    public ChatType getType() {
        return ChatType.WORLD;
    }
    
    @Override
    public UserSummary getCurrentUser() {
        return currentUser;
    }
    
    @Override
    public List<UserSummary> getParticipants() {
        // World chat không track participants cụ thể
        return Collections.emptyList();
    }
    
    @Override
    public boolean canSendMessage() {
        // Luôn cho phép gửi tin trong world chat
        return currentUser != null;
    }
    
    @Override
    public String getTitle() {
        return "World Chat";
    }
    
    @Override
    public boolean showUserStatus() {
        // Hiển thị rank/level của users
        return true;
    }
    
    @Override
    public boolean showEmoji() {
        return true;
    }
    
    @Override
    public boolean showTimestamp() {
        return true;
    }
}
