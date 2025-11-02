package com.example.memorygame.controller.chat;

import java.util.List;

import com.example.memorygame.controller.chat.contexts.LobbyChatContext;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.TCPClient;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller cho LobbyChat.fxml
 * 
 * Lobby Chat tích hợp trong lobby screen, hiển thị chat của tất cả players trong room.
 * Chat sẽ bị xóa khi room giải tán.
 */
public class LobbyChatController {
    
    @FXML
    private VBox chatContainer;
    @FXML
    private Label playersCountLabel;
    
    @FXML
    private HBox participantsContainer;
    
    
    private ChatComponent chatComponent;
    private LobbyChatContext context;
    private TCPClient tcpClient;
    
    public LobbyChatController() {
        this.tcpClient = TCPClient.getInstance();
    }
    
    @FXML
    private void initialize() {
        // Initialize sẽ được gọi sau khi FXML load
    }
    
    /**
     * Setup lobby chat với room ID và current user
     */
    public void setupLobby(String roomId, UserSummary currentUser) {
        if (roomId == null || currentUser == null) {
            throw new IllegalArgumentException("Room ID and current user cannot be null");
        }
        
        // Tạo context cho lobby chat
        context = new LobbyChatContext(roomId, currentUser);
        
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
     * Update danh sách participants trong room
     */
    public void updateParticipants(List<UserSummary> participants) {
        if (context != null && participants != null) {
            context.setParticipants(participants);
        }
    }
    
    /**
     * Thêm participant mới vào room
     */
    public void addParticipant(UserSummary user) {
        if (context != null && user != null) {
            context.addParticipant(user);
        }
    }
    
    /**
     * Remove participant khỏi room
     */
    public void removeParticipant(UserSummary user) {
        if (context != null && user != null) {
            context.removeParticipant(user);
        }
    }
    
    /**
     * Update room state (ví dụ: disable chat khi game bắt đầu)
     */
    public void setRoomActive(boolean active) {
        if (context != null) {
            context.setRoomActive(active);
        }
    }
    
    /**
     * Cleanup khi rời khỏi lobby
     */
    public void cleanup() {
        if (context != null) {
            // Unsubscribe khỏi channel
            tcpClient.unsubscribeFromChannel(context.getChannelId());
            
            // Set room không còn active
            context.setRoomActive(false);
        }
        
        // Clear UI
        if (chatContainer != null) {
            chatContainer.getChildren().clear();
        }
    }
    
    /**
     * Lấy lobby context để các components khác có thể query state
     */
    public LobbyChatContext getContext() {
        return context;
    }
}
