package com.example.memorygame.controller.chat;

import com.example.memorygame.controller.chat.contexts.LobbyChatContext;
import com.example.memorygame.controller.room.RoomStateManager;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.UserApi;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/**
 * Controller cho LobbyChat.fxml
 * Quản lý chat trong phòng chờ (lobby)
 */
public class LobbyChatController {
    
    @FXML
    private VBox chatContainer;
    
    private ChatComponent chatComponent;
    private LobbyChatContext context;
    private TCPClient tcpClient;
    private RoomStateManager roomStateManager;
    
    public LobbyChatController() {
        this.tcpClient = TCPClient.getInstance();
    }
    
    /**
     * Set RoomStateManager để có thể lấy roomId
     */
    public void setRoomStateManager(RoomStateManager stateManager) {
        this.roomStateManager = stateManager;
    }
    
    /**
     * Khởi tạo lobby chat với room ID và current user
     * Nếu roomId là null, sẽ tự động lấy từ RoomStateManager
     */
    public void setupLobby(String roomId, UserSummary currentUser) {
        if (currentUser == null) {
            throw new IllegalArgumentException("Current user cannot be null");
        }
        
        // Tự động lấy roomId từ RoomStateManager nếu chưa có
        if (roomId == null) {
            if (roomStateManager == null) {
                throw new IllegalStateException("RoomStateManager is not set. Cannot get room ID.");
            }
            Long currentRoomId = roomStateManager.getCurrentRoomId();
            if (currentRoomId == null) {
                throw new IllegalStateException("Cannot get room ID from RoomStateManager. User may not be in any room.");
            }
            roomId = String.valueOf(currentRoomId);
        }
        
        // Tạo context và chat component
        context = new LobbyChatContext(roomId, currentUser);
        chatComponent = new ChatComponent(context);
        
        // Thêm component vào container
        if (chatContainer != null) {
            chatContainer.getChildren().clear();
            chatContainer.getChildren().add(chatComponent);
        }
        
        // Đăng ký handlers cho TCP client
        tcpClient.registerChatHandlers();
    }
    
    /**
     * Khởi tạo lobby chat tự động lấy room ID từ RoomStateManager
     */
    public void setupLobby() {
        UserSummary currentUser = UserApi.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Cannot get current user");
        }
        setupLobby(null, currentUser);
    }
    
    /**
     * Cleanup khi rời khỏi lobby
     */
    public void cleanup() {
        if (context != null) {
            tcpClient.unsubscribeFromChannel(context.getChannelId());
            context.setRoomActive(false);
        }
        
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
