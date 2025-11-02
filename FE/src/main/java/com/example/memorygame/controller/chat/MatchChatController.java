package com.example.memorygame.controller.chat;

import com.example.memorygame.controller.chat.contexts.MatchChatContext;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.TCPClient;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Controller cho MatchChat.fxml
 * 
 * Match Chat tích hợp trong match screen, cho phép 2 players chat với nhau.
 * Chat sẽ bị xóa sau khi match kết thúc.
 */
public class MatchChatController {
    
    @FXML
    private VBox chatContainer;
    @FXML
    private Label matchStatusLabel;
    
    
    private ChatComponent chatComponent;
    private MatchChatContext context;
    private TCPClient tcpClient;
    
    public MatchChatController() {
        this.tcpClient = TCPClient.getInstance();
    }
    
    @FXML
    private void initialize() {
        // Initialize sẽ được gọi sau khi FXML load
    }
    
    /**
     * Setup match chat với match ID và players
     */
    public void setupMatch(String matchId, UserSummary currentUser, UserSummary opponent) {
        if (matchId == null || currentUser == null || opponent == null) {
            throw new IllegalArgumentException("Match ID, current user, and opponent cannot be null");
        }
        
        // Tạo context cho match chat
        context = new MatchChatContext(matchId, currentUser, opponent);
        
        // Tạo chat component với context
        chatComponent = new ChatComponent(context);
        
        // Thêm component vào container
        if (chatContainer != null) {
            chatContainer.getChildren().clear();
            chatContainer.getChildren().add(chatComponent);
        }
        
        // Đăng ký handlers cho TCP client nếu chưa có
        tcpClient.registerChatHandlers();
    }
    
    /**
     * Update match state (ví dụ: pause/resume)
     */
    public void setMatchActive(boolean active) {
        if (context != null) {
            context.setMatchActive(active);
        }
    }
    
    /**
     * Cleanup khi match kết thúc
     */
    public void cleanup() {
        if (context != null) {
            // Unsubscribe khỏi channel
            tcpClient.unsubscribeFromChannel(context.getChannelId());
            
            // Set match không còn active
            context.setMatchActive(false);
        }
        
        // Clear UI
        if (chatContainer != null) {
            chatContainer.getChildren().clear();
        }
    }
    
    /**
     * Lấy match context để các components khác có thể query state
     */
    public MatchChatContext getContext() {
        return context;
    }
}
