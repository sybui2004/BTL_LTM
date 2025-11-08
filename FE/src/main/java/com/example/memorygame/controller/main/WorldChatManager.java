package com.example.memorygame.controller.main;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.UserApi;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/**
 * Manages world chat functionality
 */
public class WorldChatManager {
    private final VBox chatMessagesContainer;
    private final ScrollPane chatScrollPane;
    private final UserSummary currentUser;
    private final Function<String, Image> avatarLoader;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
    private final Map<String, UserSummary> userCache = new HashMap<>();

    public WorldChatManager(VBox chatMessagesContainer, ScrollPane chatScrollPane, UserSummary currentUser, Function<String, Image> avatarLoader) {
        this.chatMessagesContainer = chatMessagesContainer;
        this.chatScrollPane = chatScrollPane;
        this.currentUser = currentUser;
        this.avatarLoader = avatarLoader;
    }

    public void loadRecentMessages() {
        // TODO: Load recent chat messages from API if needed
        // For now, just display a welcome message
        Platform.runLater(() -> {
            addSystemMessage("Welcome to Memory Game! Chat with other players here.");
        });
    }

    public void sendMessage(String message) {
        if (currentUser == null)
            return;

        String username = currentUser.username;

        // Send via TCP
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);

        TCPClient.TCPMessage tcpMessage = new TCPClient.TCPMessage(
                "WORLD_CHAT",
                data,
                username,
                null);

        TCPClient.getInstance().sendMessage(tcpMessage);
    }

    public void handleIncomingMessage(TCPClient.TCPMessage message) {
        Platform.runLater(() -> {
            Map<String, Object> data = message.getData();
            if (data != null) {
                String sender = message.getSender();
                Object messageObj = data.get("message");

                if (sender != null && messageObj != null) {
                    addChatMessage(sender, messageObj.toString(), new Date(message.getTimestamp()));
                }
            }
        });
    }

    private void addChatMessage(String sender, String message, Date timestamp) {
        if (chatMessagesContainer == null)
            return;

        // Get user info (with caching)
        UserSummary senderUser = userCache.get(sender);
        if (senderUser == null) {
            // Try to get from all users list (async, will update later)
            new Thread(() -> {
                try {
                    List<UserSummary> allUsers = UserApi.getAllUserSummaries();
                    UserSummary foundUser = null;
                    for (UserSummary user : allUsers) {
                        if (user.username != null && user.username.equals(sender)) {
                            foundUser = user;
                            break;
                        }
                    }
                    if (foundUser != null) {
                        userCache.put(sender, foundUser);
                    }
                    // Message already shown, just cache for next time
                } catch (Exception e) {
                    // Ignore
                }
            }).start();
            // Show message immediately with default info
            addChatMessageBox(sender, message, timestamp, null);
        } else {
            addChatMessageBox(sender, message, timestamp, senderUser);
        }
    }

    private void addChatMessageBox(String sender, String message, Date timestamp, UserSummary senderUser) {
        HBox messageBox = new HBox(10);
        messageBox.getStyleClass().add("chat-message");
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(8, 12, 8, 12));

        // Avatar
        ImageView avatarView = new ImageView();
        avatarView.setFitWidth(40);
        avatarView.setFitHeight(40);
        avatarView.setPreserveRatio(true);
        
        if (senderUser != null && senderUser.avatarUrl != null && avatarLoader != null) {
            avatarView.setImage(avatarLoader.apply(senderUser.avatarUrl));
        } else if (avatarLoader != null) {
            avatarView.setImage(avatarLoader.apply(null));
        }
        
        // Circular avatar
        Circle clip = new Circle(20, 20, 20);
        avatarView.setClip(clip);
        avatarView.getStyleClass().add("chat-message-avatar");

        // Message content (username + message)
        VBox contentBox = new VBox(3);
        
        // Username with colon
        Label senderLabel = new Label(sender + ":");
        senderLabel.getStyleClass().add("chat-sender");
        
        // Message text
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("chat-text");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(380);
        
        contentBox.getChildren().addAll(senderLabel, messageLabel);

        messageBox.getChildren().addAll(avatarView, contentBox);
        chatMessagesContainer.getChildren().add(messageBox);


    }

    private void addSystemMessage(String message) {
        if (chatMessagesContainer == null)
            return;

        Label systemLabel = new Label("ℹ️ " + message);
        systemLabel.getStyleClass().add("chat-text");
        systemLabel.setWrapText(true);
        systemLabel.setMaxWidth(380);
        systemLabel.setStyle("-fx-text-fill: #7F8C8D; -fx-font-style: italic;");

        chatMessagesContainer.getChildren().add(systemLabel);
;
    }


}
