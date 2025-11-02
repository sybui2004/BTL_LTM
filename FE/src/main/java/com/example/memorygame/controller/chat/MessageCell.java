package com.example.memorygame.controller.chat;

import com.example.memorygame.model.chat.ChatMessage;
import com.example.memorygame.model.user.UserSummary;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

/**
 * Custom message cell component for chat
 * Displays avatar, username, and message bubble
 */
public class MessageCell extends HBox {
    
    private final ChatMessage message;
    private final UserSummary currentUser;
    
    public MessageCell(ChatMessage message, UserSummary currentUser) {
        this.message = message;
        this.currentUser = currentUser;
        
        setupLayout();
        buildUI();
    }
    
    private void setupLayout() {
        this.setSpacing(10);
        this.setPadding(new Insets(5, 10, 5, 10));
        this.setMaxWidth(Double.MAX_VALUE);
        this.setPrefWidth(Double.MAX_VALUE); // ensure cell stretches to container width
        
        // Check if message is from current user
        boolean isMine = isMyMessage();
        
        if (isMine) {
            // Align right for own messages
            this.setAlignment(Pos.CENTER_RIGHT);
        } else {
            // Align left for other's messages
            this.setAlignment(Pos.CENTER_LEFT);
        }
    }
    
    private void buildUI() {
        boolean isMine = isMyMessage();
        
        if (isMine) {
            // Own message: [spacer] [username+bubble] [avatar]
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            VBox bubbleContainer = createContentWithUsernameForMine();
            ImageView avatar = createAvatar();
            
            this.getChildren().addAll(spacer, bubbleContainer, avatar);
        } else {
            // Other's message: [avatar] [username + bubble] [spacer]
            ImageView avatar = createAvatar();
            VBox contentContainer = createContentWithUsername();
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            this.getChildren().addAll(avatar, contentContainer, spacer);
        }
    }
    
    private ImageView createAvatar() {
        ImageView avatar = new ImageView();
        avatar.setFitWidth(40);
        avatar.setFitHeight(40);
        avatar.setPreserveRatio(true);
        
        // Clip to circle
        Circle clip = new Circle(20, 20, 20);
        avatar.setClip(clip);
        
        // Load avatar image if available
        String avatarUrl = getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            try {
                Image image = new Image(avatarUrl, true);
                avatar.setImage(image);
            } catch (Exception e) {
                // Fallback to default avatar
                setDefaultAvatar(avatar);
            }
        } else {
            setDefaultAvatar(avatar);
        }
        
        return avatar;
    }
    
    private void setDefaultAvatar(ImageView avatar) {
        try {
            // Load default avatar from assets
            Image defaultImage = new Image(
                getClass().getResourceAsStream("/com/example/memorygame/assets/images/name.png")
            );
            avatar.setImage(defaultImage);
        } catch (Exception e) {
            // If loading fails, use a solid color background
            System.err.println("Failed to load default avatar: " + e.getMessage());
            avatar.setStyle("-fx-background-color: #cccccc; -fx-background-radius: 20;");
        }
    }
    
    private VBox createContentWithUsername() {
        VBox container = new VBox(3);
        container.setAlignment(Pos.TOP_LEFT);
        
        // Username label
        Label usernameLabel = new Label(getDisplayName());
        usernameLabel.getStyleClass().add("message-username");
        usernameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-weight: bold;");
        
        // Message bubble
        VBox bubble = createBubbleContainer(false);
        
        container.getChildren().addAll(usernameLabel, bubble);
        return container;
    }

    private VBox createContentWithUsernameForMine() {
        VBox container = new VBox(3);
        container.setAlignment(Pos.TOP_RIGHT);

        // Username label (right aligned)
        Label usernameLabel = new Label(getDisplayName());
        usernameLabel.getStyleClass().add("message-username");
        usernameLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-weight: bold;");
        
        VBox bubble = createBubbleContainer(true);

        container.getChildren().addAll(usernameLabel, bubble);
        return container;
    }
    
    private VBox createBubbleContainer(boolean isMine) {
        VBox bubble = new VBox();
        bubble.setPadding(new Insets(10, 16, 10, 16));
        bubble.setMaxWidth(320);
        
        // Message content
        Label contentLabel = new Label(message.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 14px; -fx-font-family: 'Source Serif Pro', serif; -fx-text-fill: #1a1a1a;");
        
        bubble.getChildren().add(contentLabel);
        
        // Apply style class
        bubble.getStyleClass().add("chat-bubble");
        if (isMine) {
            bubble.getStyleClass().add("mine");
        } else {
            bubble.getStyleClass().add("theirs");
        }
        
        // Force white background with inline style to ensure it applies
        bubble.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 18;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0.3, 0, 1);"
        );
        
        return bubble;
    }
    
    private boolean isMyMessage() {
        if (currentUser == null || message.getSender() == null) {
            System.out.println("[MessageCell] NULL CHECK - currentUser: " + currentUser + ", sender: " + message.getSender());
            return false;
        }

        long curId = currentUser.id;
        long senderId = message.getSender().id;

        // Prefer reliable id comparison when both ids are positive
        if (curId > 0 && senderId > 0) {
            boolean same = curId == senderId;
            System.out.println("[MessageCell] Compare by ID - cur:" + curId + ", sender:" + senderId + " -> " + same);
            if (same) return true;
        }

        // Fallback: compare by username (common in realtime messages w/o id)
        String cu = currentUser.username;
        String su = message.getSender().username;
        if (cu != null && su != null && !cu.isBlank() && !su.isBlank()) {
            boolean same = cu.equalsIgnoreCase(su);
            System.out.println("[MessageCell] Compare by username - cur:" + cu + ", sender:" + su + " -> " + same);
            if (same) return true;
        }

        // Last fallback: compare by displayName if provided
        String cd = currentUser.displayName;
        String sd = message.getSender().displayName;
        if (cd != null && sd != null && !cd.isBlank() && !sd.isBlank()) {
            boolean same = cd.equalsIgnoreCase(sd);
            System.out.println("[MessageCell] Compare by displayName - cur:" + cd + ", sender:" + sd + " -> " + same);
            if (same) return true;
        }

        return false;
    }
    
    private String getDisplayName() {
        if (message.getSender() == null) {
            return "Unknown";
        }
        
        if (message.getSender().displayName != null && !message.getSender().displayName.isBlank()) {
            return message.getSender().displayName;
        }
        
        if (message.getSender().username != null && !message.getSender().username.isBlank()) {
            return message.getSender().username;
        }
        
        return "Unknown";
    }
    
    private String getAvatarUrl() {
        if (message.getSender() == null || message.getSender().avatarUrl == null) {
            return null;
        }
        return message.getSender().avatarUrl;
    }
}
