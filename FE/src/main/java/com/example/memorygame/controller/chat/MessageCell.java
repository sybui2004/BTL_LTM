package com.example.memorygame.controller.chat;

import com.example.memorygame.model.chat.ChatMessage;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.AvatarCacheManager;
import com.example.memorygame.utils.TimestampUtil;

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
 * Custom message cell component for chat.
 * Displays avatar, username, and message bubble based on the sender and layout mode.
 */
public class MessageCell extends HBox {
    public enum LayoutMode {
        DEFAULT,          // Tin nhắn của mình bên phải, của người khác bên trái.
        MINE_ON_LEFT,     // Dành cho MatchChat: Tin nhắn của mình bên trái.
        OPPONENT_ON_RIGHT // Dành cho MatchChat: Tin nhắn của đối thủ bên phải.
    }
    
    private final ChatMessage message;
    private final UserSummary currentUser;
    private final LayoutMode layoutMode;
    private final ChatMessage previousMessage;
    private final ChatContext context;
    
    public MessageCell(ChatMessage message, UserSummary currentUser, LayoutMode layoutMode, ChatMessage previousMessage) {
        this(message, currentUser, layoutMode, previousMessage, null);
    }
    
    public MessageCell(ChatMessage message, UserSummary currentUser, LayoutMode layoutMode, ChatMessage previousMessage, ChatContext context) {
        this.message = message;
        this.currentUser = currentUser;
        this.layoutMode = layoutMode != null ? layoutMode : LayoutMode.DEFAULT;
        this.previousMessage = previousMessage;
        this.context = context;
        
        setupLayout();
        buildUI();
    }
    
    private void setupLayout() {
        setSpacing(10);
        setPadding(new Insets(5, 0, 5, 0));
        setMaxWidth(Double.MAX_VALUE);
        
        boolean isMine = isMyMessage();
        
        if ((isMine && layoutMode == LayoutMode.DEFAULT) || (!isMine && layoutMode == LayoutMode.OPPONENT_ON_RIGHT)) {
            setAlignment(Pos.CENTER_RIGHT);
        } else {
            setAlignment(Pos.TOP_LEFT);
        }
    }
    
    private void buildUI() {
        boolean isMine = isMyMessage();
        
        if (isMine) {
            // Tin nhắn của bản thân
            Region spacer = new Region();
            VBox bubbleContainer = createOwnMessageContainer(true);
            
            if (layoutMode == LayoutMode.DEFAULT) {
                HBox.setHgrow(spacer, Priority.ALWAYS);
                getChildren().addAll(spacer, bubbleContainer);
            } else { // MINE_ON_LEFT
                HBox.setHgrow(spacer, Priority.ALWAYS);
                getChildren().addAll(bubbleContainer, spacer);
            }
        } else {
            // Tin nhắn của người khác
            if (layoutMode == LayoutMode.OPPONENT_ON_RIGHT) {
                // Tin nhắn đối thủ trong MatchChat (chỉ có bubble)
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                VBox bubbleOnly = createBubbleContainer(false);
                getChildren().addAll(spacer, bubbleOnly);
            } else {
                // Tin nhắn người khác trong các chat khác (đầy đủ avatar, tên)
                ImageView avatar = createAvatar();
                VBox contentContainer = createContentWithUsername(false);
                
                VBox avatarWrapper = new VBox(avatar);
                avatarWrapper.setAlignment(Pos.TOP_LEFT);
                
                Region spacer = new Region();
                HBox.setHgrow(spacer, Priority.ALWAYS);
                
                getChildren().addAll(avatarWrapper, contentContainer, spacer);
            }
        }
    }
    
    private ImageView createAvatar() {
        ImageView avatarView = new ImageView();
        
        // Đặt kích thước cố định để đảm bảo hình tròn
        avatarView.setFitWidth(44);
        avatarView.setFitHeight(44);
        
        // Không preserve ratio để ảnh fill đầy đủ vùng 44x44
        // Sau đó sẽ clip bằng circle để tạo hình tròn hoàn hảo
        avatarView.setPreserveRatio(false);
        
        // Tối ưu hiển thị
        avatarView.setSmooth(true);
        avatarView.setCache(true);
        
        // Tạo clip hình tròn ở center với bán kính 22 (đường kính 44)
        Circle clip = new Circle(22, 22, 22);
        avatarView.setClip(clip);
        
        AvatarCacheManager cacheManager = AvatarCacheManager.getInstance();
        Image avatarImage = cacheManager.getAvatar(getAvatarUrl());
        
        avatarView.setImage(avatarImage != null ? avatarImage : cacheManager.getDefaultAvatar());
        
        // ImageView tự động cập nhật khi Image tải trong nền.
        // AvatarCacheManager xử lý lỗi và trả về ảnh mặc định.
        
        // Thêm click handler để mở profile (chỉ cho world chat và không phải tin nhắn của mình)
        if (context != null && context.getType() == com.example.memorygame.model.chat.ChatType.WORLD && !isMyMessage()) {
            avatarView.setCursor(javafx.scene.Cursor.HAND);
            avatarView.setOnMouseClicked(e -> {
                UserSummary sender = message.getSender();
                if (sender != null && sender.id > 0) {
                    openProfile(sender.id);
                }
            });
        }
        
        return avatarView;
    }
    
    /**
     * Mở profile screen cho user
     */
    private void openProfile(Long userId) {
        if (userId == null || userId <= 0) {
            return;
        }
        
        try {
            javafx.application.Platform.runLater(() -> {
                try {
                    com.example.memorygame.controller.ProfileScreenController profileController = 
                        new com.example.memorygame.controller.ProfileScreenController(userId);
                    javafx.scene.Scene profileScene = new javafx.scene.Scene(profileController.getScreen().getRoot());
                    
                    // Lấy stage từ scene hiện tại
                    javafx.scene.Node node = this;
                    javafx.stage.Stage stage = null;
                    while (node != null && stage == null) {
                        if (node.getScene() != null && node.getScene().getWindow() instanceof javafx.stage.Stage) {
                            stage = (javafx.stage.Stage) node.getScene().getWindow();
                            break;
                        }
                        node = node.getParent();
                    }
                    
                    if (stage != null) {
                        // Preserve current stage size
                        double currentWidth = stage.getWidth();
                        double currentHeight = stage.getHeight();
                        if (currentWidth > 0 && currentHeight > 0) {
                            stage.setWidth(currentWidth);
                            stage.setHeight(currentHeight);
                        } else {
                            stage.setWidth(1024);
                            stage.setHeight(720);
                        }
                        stage.setScene(profileScene);
                    }
                } catch (Exception ex) {
                    System.err.println("[MessageCell] Failed to open profile: " + ex.getMessage());
                    ex.printStackTrace();
                }
            });
        } catch (Exception e) {
            System.err.println("[MessageCell] Error opening profile: " + e.getMessage());
        }
    }
    
    /**
     * Tạo container cho tin nhắn của chính mình (chỉ có bubble và timestamp).
     */
    private VBox createOwnMessageContainer(boolean alignRight) {
        VBox container = new VBox(3);
        container.setAlignment(alignRight ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        
        boolean showTimestamp = layoutMode == LayoutMode.DEFAULT && TimestampUtil.shouldShowTimestamp(
            message.getTimestamp(), 
            previousMessage != null ? previousMessage.getTimestamp() : null
        );
        
        if (showTimestamp && message.getTimestamp() != null) {
            Label timestampLabel = new Label(TimestampUtil.formatMessageTimestamp(message.getTimestamp()));
            timestampLabel.getStyleClass().add("message-timestamp");
            timestampLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888; -fx-font-style: italic;");
            
            if (alignRight) {
                timestampLabel.setPadding(new Insets(5, 13, 2, 0));
                timestampLabel.setAlignment(Pos.CENTER_RIGHT);
            } else {
                timestampLabel.setPadding(new Insets(5, 0, 2, 13));
                timestampLabel.setAlignment(Pos.CENTER_LEFT);
            }
            container.getChildren().add(timestampLabel);
        }
        
        VBox bubble = createBubbleContainer(true);
        container.getChildren().add(bubble);
        
        return container;
    }

    /**
     * Tạo container cho tin nhắn của người khác (tên, bubble, và timestamp).
     */
    private VBox createContentWithUsername(boolean alignRight) {
        VBox container = new VBox(3);
        container.setAlignment(alignRight ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        
        boolean showTimestamp = layoutMode == LayoutMode.DEFAULT && TimestampUtil.shouldShowTimestamp(
            message.getTimestamp(), 
            previousMessage != null ? previousMessage.getTimestamp() : null
        );
        
        Label usernameLabel = new Label(getDisplayName());
        usernameLabel.getStyleClass().add("message-username");
        usernameLabel.setStyle("-fx-font-size: 17px; -fx-text-fill: #000; -fx-font-weight: bold; -fx-font-family: 'VT323', 'Consolas', 'Courier New', monospace;");
        usernameLabel.setPadding(alignRight ? new Insets(0, 13, 0, 0) : new Insets(0, 0, 0, 25));
        
        VBox bubble = createBubbleContainer(false);
        
        container.getChildren().addAll(usernameLabel, bubble);
        
        if (showTimestamp && message.getTimestamp() != null) {
            Label timestampLabel = new Label(TimestampUtil.formatMessageTimestamp(message.getTimestamp()));
            timestampLabel.getStyleClass().add("message-timestamp");
            timestampLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888; -fx-font-style: italic;");
            
            timestampLabel.setPadding(new Insets(2, 5, 0, 0));
            timestampLabel.setAlignment(Pos.CENTER_RIGHT);
            timestampLabel.setMaxWidth(Double.MAX_VALUE);
            
            container.getChildren().add(timestampLabel);
        }
        
        return container;
    }
    
    private VBox createBubbleContainer(boolean isMine) {
        VBox bubble = new VBox();
        
        if (message.getMessageType() == com.example.memorygame.model.chat.MessageType.STICKER) {
            bubble.setPadding(new Insets(0));
            bubble.setMaxWidth(100);
            
            ImageView stickerImageView = new ImageView();
            stickerImageView.setFitWidth(60);
            stickerImageView.setFitHeight(60);
            stickerImageView.setPreserveRatio(true);
            stickerImageView.setSmooth(true);
            
            String stickerPath = message.getStickerPath();
            if (stickerPath != null && !stickerPath.isBlank()) {
                try {
                    String imageUrl = stickerPath;
                    if (!imageUrl.startsWith("http")) {
                        imageUrl = "http://localhost:8080" + (imageUrl.startsWith("/") ? "" : "/static/stickers/") + imageUrl;
                    }
                    stickerImageView.setImage(new Image(imageUrl, true));
                } catch (Exception e) {
                    bubble.getChildren().add(new Label("Sticker"));
                }
            } else {
                bubble.getChildren().add(new Label("Sticker"));
            }
            
            if (stickerImageView.getImage() != null) {
                bubble.getChildren().add(stickerImageView);
            }
        } else {
            bubble.setPadding(new Insets(8, 16, 8, 16));
            bubble.setMaxWidth(320);
            
            Label contentLabel = new Label(message.getContent());
            contentLabel.setWrapText(true);

            String fontFamily = MatchChatController.getSourceSerif4Family();
            if (fontFamily != null && !fontFamily.isEmpty()) {
                contentLabel.setFont(javafx.scene.text.Font.font(fontFamily, 17));
                contentLabel.setStyle("-fx-text-fill: #000000;");
            } else {
                contentLabel.setStyle("-fx-font-size: 17px; -fx-font-family: serif; -fx-text-fill: #000000;");
            }
            
            bubble.getChildren().add(contentLabel);
            
            bubble.getStyleClass().addAll("chat-bubble", isMine ? "mine" : "theirs");
            
            bubble.setStyle(
                "-fx-background-color: white;" +
                "-fx-background-radius: 18;" +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0.3, 0, 1);"
            );
        }
        
        return bubble;
    }
    
    private boolean isMyMessage() {
        if (currentUser == null || message.getSender() == null) {
            return false;
        }

        if (currentUser.id > 0 && message.getSender().id > 0) {
            return currentUser.id == message.getSender().id;
        }

        if (currentUser.username != null && !currentUser.username.isBlank()) {
            return currentUser.username.equalsIgnoreCase(message.getSender().username);
        }

        return false;
    }
    
    private String getDisplayName() {
        UserSummary sender = message.getSender();
        if (sender == null) return "Unknown";
        if (sender.displayName != null && !sender.displayName.isBlank()) return sender.displayName;
        if (sender.username != null && !sender.username.isBlank()) return sender.username;
        return "Unknown";
    }
    
    private String getAvatarUrl() {
        UserSummary sender = message.getSender();
        if (sender == null || sender.avatarUrl == null || sender.avatarUrl.isBlank() || "null".equals(sender.avatarUrl)) {
            return null;
        }
        return sender.avatarUrl;
    }

    public ChatMessage getMessage() {
        return message;
    }
}
