package com.example.memorygame.controller.chat;

import com.example.memorygame.controller.chat.contexts.WorldChatContext;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.TCPClient;

import javafx.fxml.FXML;
import javafx.scene.layout.VBox;

/**
 * Controller cho WorldChat.fxml
 * Quản lý World Chat - chat toàn server cho tất cả người chơi
 */
public class WorldChatController {
    
    @FXML
    private VBox chatContainer;
    
    private ChatComponent chatComponent;
    private WorldChatContext context;
    private TCPClient tcpClient;
    
    public WorldChatController() {
        this.tcpClient = TCPClient.getInstance();
    }
    
    @FXML
    private VBox root;
    
    @FXML
    private void initialize() {
        // Set background image cho WorldChat
        if (root != null) {
            try {
                java.net.URL bgUrl = getClass().getResource("/com/example/memorygame/assets/images/bg_1.png");
                if (bgUrl != null) {
                    String bgStyle = String.format(
                        "-fx-background-image: url('%s'); -fx-background-size: cover; -fx-background-repeat: no-repeat; -fx-background-position: center center;",
                        bgUrl.toExternalForm()
                    );
                    root.setStyle(bgStyle);
                } else {
                    System.err.println("Background image not found: /com/example/memorygame/assets/images/bg_1.png");
                }
            } catch (Exception e) {
                System.err.println("Could not load background image: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Setup World Chat với user hiện tại
     * Chỉ tạo context và ChatComponent một lần để tránh flicker
     */
    public void setCurrentUser(UserSummary currentUser) {
        if (currentUser == null) return;
        
        // Chỉ khởi tạo một lần, tái sử dụng khi user thay đổi
        if (context == null) {
            context = new WorldChatContext(currentUser);
            chatComponent = new ChatComponent(context);
            
            if (chatContainer != null) {
                chatContainer.getChildren().clear();
                chatContainer.getChildren().add(chatComponent);
            }
            
            tcpClient.registerChatHandlers();
            
            // Subscribe vào channel để nhận tin nhắn World Chat
            tcpClient.subscribeToChannel(context.getChannelId(), msg -> {
                javafx.application.Platform.runLater(() -> {
                    if (chatComponent != null) {
                        // Preload avatar để tối ưu hiển thị
                        if (msg.getSender() != null && msg.getSender().avatarUrl != null) {
                            com.example.memorygame.utils.AvatarCacheManager.getInstance()
                                .preloadAvatar(msg.getSender().avatarUrl);
                        }
                        chatComponent.addMessage(msg);
                    }
                });
            });
        }
    }

    /**
     * Cleanup khi đóng World Chat
     * Unsubscribe khỏi channel để tránh memory leak
     */
    public void cleanup() {
        if (context != null && chatComponent != null) {
            tcpClient.unsubscribeFromChannel(context.getChannelId());
        }
    }
}
