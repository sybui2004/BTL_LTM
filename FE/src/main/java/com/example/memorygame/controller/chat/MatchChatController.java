package com.example.memorygame.controller.chat;

import java.util.UUID;

import com.example.memorygame.controller.chat.contexts.MatchChatContext;
import com.example.memorygame.controller.room.RoomStateManager;
import com.example.memorygame.model.chat.ChatMessage;
import com.example.memorygame.model.chat.ChatType;
import com.example.memorygame.model.chat.MessageType;
import com.example.memorygame.model.chat.Sticker;
import com.example.memorygame.model.game.GameSettings;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.SoundManager;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.UserApi;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;

/**
 * Controller cho MatchChat.fxml (layout góc màn hình)
 * - Góc dưới trái: tin của người gửi (căn trái) + ô nhập, tin mới nhất lên trên
 * - Góc trên phải: tin của đối thủ (căn phải), tin mới nhất xuống dưới, không có ô nhập
 * - Giữa màn hình để trống cho game cards
 */
public class MatchChatController {

    // Chat panes
    @FXML private VBox rootBox;
    @FXML private javafx.scene.layout.AnchorPane chatAnchor;
    @FXML private StackPane leftPane;
    @FXML private StackPane rightPane;
    @FXML private AnchorPane leftAnimationLayer;
    @FXML private AnchorPane rightAnimationLayer;
    @FXML private ScrollPane leftScroll;
    @FXML private ScrollPane rightScroll;
    @FXML private VBox leftMessageContainer;
    @FXML private VBox rightMessageContainer;

    // Input (left only)
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private Button stickerMatchButton;
    @FXML private VBox leftPlayerInputContainer;
    @FXML private javafx.scene.layout.HBox leftInputArea;
    
    // Player headers
    @FXML private Label leftPlayerName;
    @FXML private ImageView leftPlayerAvatar;
    @FXML private Label rightPlayerName;
    @FXML private ImageView rightPlayerAvatar;

    private MatchChatContext context;
    private TCPClient tcpClient;
    private RoomStateManager roomStateManager;

    public MatchChatController() {
        this.tcpClient = TCPClient.getInstance();
    }
    
    /**
     * Set RoomStateManager để có thể lấy roomId
     */
    public void setRoomStateManager(RoomStateManager stateManager) {
        this.roomStateManager = stateManager;
    }

    @FXML
    private void initialize() {
        if (sendButton != null) sendButton.setOnAction(e -> sendMessage());
        if (inputField != null) inputField.setOnAction(e -> sendMessage());
        if (stickerMatchButton != null) stickerMatchButton.setOnAction(e -> showStickerPicker());

        // Load fonts
        loadVT323Font();
        loadSourceSerif4Font();

        // Thiết lập container tin nhắn bên trái: tin mới nhất ở trên cùng
        if (leftMessageContainer != null) {
            leftMessageContainer.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        }
        
        if (leftScroll != null) {
            leftScroll.setFitToWidth(true);
        }
        
        // Thiết lập mouse transparent cho các container
        if (rootBox != null) {
            rootBox.setPickOnBounds(false);
            rootBox.setMouseTransparent(false);
            rootBox.setStyle("-fx-background-color: transparent;");
        }
        if (chatAnchor != null) {
            chatAnchor.setPickOnBounds(false);
            chatAnchor.setMouseTransparent(false);
            chatAnchor.setStyle("-fx-background-color: transparent;");
        }
        
        // Chat panes - cho phép tương tác
        if (leftPane != null) {
            leftPane.setPickOnBounds(true);
            leftPane.setMouseTransparent(false);
        }
        if (rightPane != null) {
            rightPane.setPickOnBounds(true);
            rightPane.setMouseTransparent(false);
        }
        
        if (leftPlayerInputContainer != null) {
            leftPlayerInputContainer.setPickOnBounds(false);
            leftPlayerInputContainer.setMouseTransparent(false);
        }
        if (leftMessageContainer != null) {
            leftMessageContainer.setPickOnBounds(false);
            leftMessageContainer.setMouseTransparent(false);
        }
        if (rightMessageContainer != null) {
            rightMessageContainer.setPickOnBounds(false);
            rightMessageContainer.setMouseTransparent(false);
        }
        
        // ScrollPane cho phép scroll
        if (leftScroll != null) {
            leftScroll.setPickOnBounds(true);
            leftScroll.setMouseTransparent(false);
        }
        if (rightScroll != null) {
            rightScroll.setPickOnBounds(true);
            rightScroll.setMouseTransparent(false);
        }

        // Tự động cuộn khi có tin nhắn mới
        if (rightMessageContainer != null && rightScroll != null) {
            rightMessageContainer.heightProperty().addListener((obs, oldHeight, newHeight) -> {
                // Cuộn xuống dưới cùng khi có tin nhắn mới (chiều cao tăng)
                if (newHeight.doubleValue() > oldHeight.doubleValue()) {
                    Platform.runLater(() -> rightScroll.setVvalue(1.0));
                }
            });
        }
        if (leftMessageContainer != null && leftScroll != null) {
            leftMessageContainer.heightProperty().addListener((obs, oldHeight, newHeight) -> {
                // Cuộn lên trên cùng khi có tin nhắn mới (tin nhắn được thêm vào đầu)
                Platform.runLater(() -> leftScroll.setVvalue(0.0));
            });
        }
        
        // Các component tương tác
        if (inputField != null) {
            inputField.setPickOnBounds(true);
            inputField.setMouseTransparent(false);
        }
        if (sendButton != null) {
            sendButton.setPickOnBounds(true);
            sendButton.setMouseTransparent(false);
        }
        if (leftInputArea != null) {
            leftInputArea.setPickOnBounds(true);
            leftInputArea.setMouseTransparent(false);
        }
        
        // Player headers
        if (leftPlayerAvatar != null) {
            leftPlayerAvatar.setPickOnBounds(true);
            leftPlayerAvatar.setMouseTransparent(false);
        }
        if (rightPlayerAvatar != null) {
            rightPlayerAvatar.setPickOnBounds(true);
            rightPlayerAvatar.setMouseTransparent(false);
        }
        if (leftPlayerName != null) {
            leftPlayerName.setPickOnBounds(true);
            leftPlayerName.setMouseTransparent(false);
        }
        if (rightPlayerName != null) {
            rightPlayerName.setPickOnBounds(true);
            rightPlayerName.setMouseTransparent(false);
        }
        
        // Thiết lập style sau khi scene được render
        Platform.runLater(() -> {
            setViewportTransparent(leftScroll);
            setViewportTransparent(rightScroll);
            
            if (rightMessageContainer != null) {
                rightMessageContainer.setStyle(
                    "-fx-padding: 2 0 2 0;" +
                    "-fx-alignment: TOP_RIGHT;"
                );
                rightMessageContainer.requestLayout();
            }
            
            styleScrollbars();
        });
    }
    
    /**
     * Thiết lập background trong suốt cho viewport của ScrollPane
     */
    private void setViewportTransparent(ScrollPane scrollPane) {
        if (scrollPane == null) return;
        
        javafx.scene.Node viewport = scrollPane.lookup(".viewport");
        if (viewport != null) {
            viewport.setStyle("-fx-background-color: transparent;");
        } else {
            Platform.runLater(() -> {
                javafx.scene.Node vp = scrollPane.lookup(".viewport");
                if (vp != null) {
                    vp.setStyle("-fx-background-color: transparent;");
                }
            });
        }
    }
    
    /**
     * Thiết lập style cho scrollbar
     */
    private void styleScrollbars() {
        styleScrollbar(leftScroll);
        styleScrollbar(rightScroll);
    }
    
    private void styleScrollbar(ScrollPane scrollPane) {
        if (scrollPane == null) return;
        
        javafx.scene.Node scrollBar = scrollPane.lookup(".scroll-bar:vertical");
        if (scrollBar != null) {
            scrollBar.setStyle(
                "-fx-background-color: transparent; " +
                "-fx-pref-width: 6px; " +
                "-fx-padding: 0;"
            );
            
            javafx.scene.Node thumb = scrollBar.lookup(".thumb");
            if (thumb != null) {
                thumb.setStyle(
                    "-fx-background-color: rgba(0, 0, 0, 0.15); " +
                    "-fx-background-radius: 3px; " +
                    "-fx-min-width: 4px;"
                );
            }
            
            javafx.scene.Node track = scrollBar.lookup(".track");
            if (track != null) {
                track.setStyle("-fx-background-color: transparent;");
            }
            
            javafx.scene.Node incButton = scrollBar.lookup(".increment-button");
            if (incButton != null) {
                incButton.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-pref-height: 0; " +
                    "-fx-min-height: 0; " +
                    "-fx-max-height: 0;"
                );
            }
            
            javafx.scene.Node decButton = scrollBar.lookup(".decrement-button");
            if (decButton != null) {
                decButton.setStyle(
                    "-fx-background-color: transparent; " +
                    "-fx-pref-height: 0; " +
                    "-fx-min-height: 0; " +
                    "-fx-max-height: 0;"
                );
            }
        } else {
            Platform.runLater(() -> styleScrollbar(scrollPane));
        }
    }
    
    private static String sourceSerif4Family = null;
    
    /**
     * Load font Source Serif 4 cho nội dung tin nhắn
     */
    private void loadSourceSerif4Font() {
        if (sourceSerif4Family != null) {
            return;
        }
        try {
            Font loadedFont = Font.loadFont(
                getClass().getResourceAsStream("/com/example/memorygame/assets/fonts/SourceSerif4-VariableFont_opsz,wght.ttf"),
                17
            );
            if (loadedFont != null) {
                sourceSerif4Family = loadedFont.getFamily();
            }
        } catch (Exception e) {
            System.err.println("[MatchChat] Failed to load Source Serif 4 font: " + e.getMessage());
            sourceSerif4Family = "serif";
        }
    }
    
    /**
     * Lấy font family Source Serif 4 (load nếu chưa load)
     */
    public static String getSourceSerif4Family() {
        if (sourceSerif4Family == null) {
            try {
                Font loadedFont = Font.loadFont(
                    MatchChatController.class.getResourceAsStream("/com/example/memorygame/assets/fonts/SourceSerif4-VariableFont_opsz,wght.ttf"),
                    17
                );
                if (loadedFont != null) {
                    sourceSerif4Family = loadedFont.getFamily();
                } else {
                    sourceSerif4Family = "serif";
                }
            } catch (Exception e) {
                System.err.println("[MatchChat] Failed to load Source Serif 4 font: " + e.getMessage());
                sourceSerif4Family = "serif";
            }
        }
        return sourceSerif4Family;
    }
    
    /**
     * Load font VT323 cho tên người chơi
     */
    private void loadVT323Font() {
        try {
            Font loadedFont = Font.loadFont(
                getClass().getResourceAsStream("/com/example/memorygame/assets/fonts/VT323-Regular.ttf"),
                20
            );
            if (loadedFont != null) {
                String fontFamily = loadedFont.getFamily();
                Platform.runLater(() -> {
                    if (leftPlayerName != null) {
                        leftPlayerName.setFont(Font.font(fontFamily, 20));
                    }
                    if (rightPlayerName != null) {
                        rightPlayerName.setFont(Font.font(fontFamily, 20));
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("[MatchChat] Failed to load VT323 font: " + e.getMessage());
        }
    }

    /**
     * Thiết lập match chat với match ID và 2 người chơi
     * Nếu matchId là null, tự động lấy từ RoomStateManager (matchId = roomId)
     */
    public void setupMatch(String matchId, UserSummary currentUser, UserSummary opponent) {
        if (currentUser == null || opponent == null) {
            throw new IllegalArgumentException("Current user and opponent cannot be null");
        }
        
        // Tự động lấy matchId từ RoomStateManager nếu chưa có
        if (matchId == null) {
            if (roomStateManager == null) {
                throw new IllegalStateException("RoomStateManager is not set. Cannot get room ID.");
            }
            Long currentRoomId = roomStateManager.getCurrentRoomId();
            if (currentRoomId == null) {
                throw new IllegalStateException("Cannot get room ID from RoomStateManager. User may not be in any room.");
            }
            matchId = String.valueOf(currentRoomId);
        }

        context = new MatchChatContext(matchId, currentUser, opponent);

        updatePlayerHeaders(currentUser, opponent);

        tcpClient.registerChatHandlers();
        try {
            tcpClient.subscribeToChannel(context.getChannelId(), msg -> Platform.runLater(() -> addMessage(msg)));
        } catch (Exception ignored) { }
    }
    
    /**
     * Thiết lập match chat từ GameSettings và RoomStateManager
     * Tự động lấy thông tin currentUser và opponent từ room state
     */
    public void setupMatchFromGameSettings(GameSettings gameSettings, RoomStateManager roomStateManager) {
        if (gameSettings == null) {
            throw new IllegalArgumentException("GameSettings cannot be null");
        }
        if (roomStateManager == null) {
            throw new IllegalArgumentException("RoomStateManager cannot be null");
        }
        
        UserSummary currentUser = UserApi.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("Cannot get current user");
        }
        
        // Lấy opponent ID từ room state
        Long opponentId;
        if (gameSettings.isHost()) {
            opponentId = roomStateManager.getCurrentGuestId();
        } else {
            opponentId = roomStateManager.getCurrentHostId();
        }
        
        if (opponentId == null) {
            throw new IllegalStateException("Cannot get opponent ID from room state");
        }
        
        UserSummary opponent = UserApi.getUserById(opponentId);
        if (opponent == null) {
            throw new IllegalStateException("Cannot get opponent user information");
        }
        
        // Lấy roomId/matchId
        String matchId = gameSettings.getRoomId() != null 
            ? String.valueOf(gameSettings.getRoomId())
            : (roomStateManager.getCurrentRoomId() != null 
                ? String.valueOf(roomStateManager.getCurrentRoomId())
                : null);
        
        setupMatch(matchId, currentUser, opponent);
    }
    
    /**
     * Cập nhật header người chơi với avatar và tên
     */
    private void updatePlayerHeaders(UserSummary currentUser, UserSummary opponent) {
        Platform.runLater(() -> {
            // Cập nhật người chơi bên trái (current user)
            if (leftPlayerName != null && currentUser != null) {
                String displayName = currentUser.displayName != null && !currentUser.displayName.isBlank() 
                    ? currentUser.displayName 
                    : currentUser.username;
                leftPlayerName.setText(displayName);
            }
            
            if (leftPlayerAvatar != null && currentUser != null) {
                loadAvatar(leftPlayerAvatar, currentUser.avatarUrl);
            }
            
            // Cập nhật người chơi bên phải (opponent)
            if (rightPlayerName != null && opponent != null) {
                String displayName = opponent.displayName != null && !opponent.displayName.isBlank() 
                    ? opponent.displayName 
                    : opponent.username;
                rightPlayerName.setText(displayName);
            }
            
            if (rightPlayerAvatar != null && opponent != null) {
                loadAvatar(rightPlayerAvatar, opponent.avatarUrl);
            }
        });
    }
    
    /**
     * Load avatar với fallback
     */
    private void loadAvatar(ImageView imageView, String avatarUrl) {
        if (imageView == null) return;
        
        // Tạo hình tròn cho avatar (48x48 -> radius = 24)
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(24, 24, 24);
        imageView.setClip(clip);
        
        String imageUrl;
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            if (!avatarUrl.startsWith("http://") && !avatarUrl.startsWith("https://")) {
                if (avatarUrl.startsWith("/static/")) {
                    imageUrl = "http://localhost:8080" + avatarUrl;
                } else if (!avatarUrl.startsWith("/")) {
                    imageUrl = "http://localhost:8080/static/avatars/" + avatarUrl;
                } else {
                    imageUrl = "http://localhost:8080" + avatarUrl;
                }
            } else {
                imageUrl = avatarUrl;
            }
        } else {
            imageUrl = "http://localhost:8080/static/avatars/default_avatar.png";
        }
        
        try {
            Image image = new Image(imageUrl, true);
            image.errorProperty().addListener((obs, wasError, isError) -> {
                if (isError) {
                    setDefaultAvatar(imageView);
                }
            });
            imageView.setImage(image);
        } catch (Exception e) {
            System.err.println("[MatchChat] Error loading avatar: " + e.getMessage());
            setDefaultAvatar(imageView);
        }
    }
    
    private void setDefaultAvatar(ImageView imageView) {
        if (imageView.getClip() == null) {
            javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(24, 24, 24);
            imageView.setClip(clip);
        }
        
        try {
            Image defaultImage = new Image(
                getClass().getResourceAsStream("/com/example/memorygame/assets/images/name.png")
            );
            imageView.setImage(defaultImage);
        } catch (Exception e) {
            System.err.println("[MatchChat] Failed to load default avatar: " + e.getMessage());
        }
    }

    private boolean isMyMessage(ChatMessage msg) {
        UserSummary cur = context != null ? context.getCurrentUser() : null;
        if (cur == null || msg == null || msg.getSender() == null) return false;
        if (cur.id > 0 && msg.getSender().id > 0) return cur.id == msg.getSender().id;
        String cu = cur.username, su = msg.getSender().username;
        return cu != null && su != null && cu.equalsIgnoreCase(su);
    }

    private void addMessage(ChatMessage msg) {
        if (leftMessageContainer == null || rightMessageContainer == null) return;

        // Nếu là sticker, chỉ thực hiện animation, KHÔNG render vào message area
        if (msg.getMessageType() == MessageType.STICKER && msg.getStickerId() != null) {
            boolean mine = isMyMessage(msg);
            Sticker sticker = new Sticker();
            sticker.setId(Long.parseLong(msg.getStickerId()));
            sticker.setStickerPath(msg.getStickerPath());
            if (mine) {
                // Sticker của mình: đã được animate khi gửi, không cần render lại
                return;
            } else {
                // Sticker của đối thủ: animate vào panel phải
                receiveSticker(sticker);
                return;
            }
        }

        // Chỉ render tin nhắn văn bản vào message area
        MessageCell cell;
        boolean mine = isMyMessage(msg);
        if (mine) {
            cell = new MessageCell(msg, context != null ? context.getCurrentUser() : null,
                    MessageCell.LayoutMode.MINE_ON_LEFT, null);
            leftMessageContainer.getChildren().add(0, cell);
        } else {
            cell = new MessageCell(msg, context != null ? context.getCurrentUser() : null,
                    MessageCell.LayoutMode.OPPONENT_ON_RIGHT, null);
            rightMessageContainer.getChildren().add(cell);
        }
    }

    private void sendMessage() {
        if (context == null || inputField == null) return;
        String content = inputField.getText() != null ? inputField.getText().trim() : "";
        if (content.isEmpty()) return;
        
        SoundManager.playSound("message.wav");

        ChatMessage message = new ChatMessage(
            UUID.randomUUID().toString(),
            content,
            context.getCurrentUser(),
            context.getChannelId(),
            ChatType.MATCH
        );

        tcpClient.sendChatMessage(message);
        inputField.clear();
    }

    /**
     * Cập nhật trạng thái match (ví dụ: pause/resume)
     */
    public void setMatchActive(boolean active) {
        if (context != null) context.setMatchActive(active);
    }

    /**
     * Dọn dẹp khi match kết thúc
     */
    public void cleanup() {
        if (context != null) {
            tcpClient.unsubscribeFromChannel(context.getChannelId());
            context.setMatchActive(false);
        }
        
        if (leftMessageContainer != null) {
            leftMessageContainer.getChildren().clear();
            if (leftScroll != null) leftScroll.setVvalue(0.0);
        }
        if (rightMessageContainer != null) {
            rightMessageContainer.getChildren().clear();
            if (rightScroll != null) rightScroll.setVvalue(1.0);
        }
    }

    public MatchChatContext getContext() { 
        return context; 
    }

    public StackPane getLeftPane() {
        return this.leftPane;
    }
    
    public StackPane getRightPane() {
        return this.rightPane;
    }

    /**
     * Hiển thị StickerPicker popup khi click button sticker
     */
    private void showStickerPicker() {
        if (context == null) return;

        StickerPicker picker = new StickerPicker("MATCH");
        picker.setOnStickerSelected(this::onStickerSelected);

        // Hiển thị popup tại vị trí button
        if (stickerMatchButton != null && stickerMatchButton.getScene() != null) {
            javafx.stage.Popup popup = new javafx.stage.Popup();
            popup.getContent().add(picker);
            popup.setAutoHide(true);

            // Tính vị trí hiển thị popup
            double x = stickerMatchButton.localToScreen(stickerMatchButton.getBoundsInLocal()).getMinX();
            double y = stickerMatchButton.localToScreen(stickerMatchButton.getBoundsInLocal()).getMaxY();
            popup.show(stickerMatchButton.getScene().getWindow(), x, y);
        }
    }

    /**
     * Xử lý khi chọn sticker từ picker
     */
    public void onStickerSelected(Sticker sticker) {
        if (sticker == null || context == null) return;

        // Phát âm thanh
        SoundManager.playSound("throw_sticker.mp3");

        // Tạo ImageView cho sticker
        ImageView stickerView = new ImageView();
        try {
            String imageUrl = sticker.getStickerPath();
            if (imageUrl != null && !imageUrl.isBlank()) {
                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                    if (imageUrl.startsWith("/static/")) {
                        imageUrl = "http://localhost:8080" + imageUrl;
                    } else if (!imageUrl.startsWith("/")) {
                        imageUrl = "http://localhost:8080/static/sticker_match/" + imageUrl;
                    } else {
                        imageUrl = "http://localhost:8080" + imageUrl;
                    }
                }
                Image image = new Image(imageUrl, true);
                stickerView.setImage(image);
            }
        } catch (Exception e) {
            System.err.println("[MatchChat] Failed to load sticker image: " + e.getMessage());
            return;
        }

        stickerView.setFitWidth(70);
        stickerView.setFitHeight(70);
        stickerView.setPreserveRatio(true);

        // Đảm bảo animationLayer nằm trên cùng trong leftPane (StackPane)
        if (leftPane != null && leftAnimationLayer != null) {
            leftPane.getChildren().remove(leftAnimationLayer);
            leftPane.getChildren().add(leftAnimationLayer);
        }
        // Đảm bảo animation chạy sau khi layout đã render
        Platform.runLater(() -> {
            MatchStickerAnimator.animate(
                stickerView,
                leftAnimationLayer, // animationLayer
                stickerMatchButton, // startNode
                leftMessageContainer, // endNode (điểm đích trong panel)
                true, // isFromMe
                v -> {} // onFinish
            );
        });

        // Gửi sticker qua TCP
        sendSticker(sticker);
    }

    /**
     * Gửi sticker qua TCP
     */
    private void sendSticker(Sticker sticker) {
        if (context == null || tcpClient == null) return;

        try {
            // Tạo ChatMessage cho sticker
            ChatMessage stickerMessage = new ChatMessage(
                UUID.randomUUID().toString(),
                "", // content rỗng cho sticker
                context.getCurrentUser(),
                context.getChannelId(),
                ChatType.MATCH
            );
            stickerMessage.setStickerId(String.valueOf(sticker.getId()));
            stickerMessage.setStickerPath(sticker.getStickerPath());
            stickerMessage.setMessageType(MessageType.STICKER);

            // Gửi qua TCP
            tcpClient.sendChatMessage(stickerMessage);
        } catch (Exception e) {
            System.err.println("[MatchChat] Failed to send sticker: " + e.getMessage());
        }
    }

    /**
     * Nhận sticker từ đối thủ và thực hiện animation
     */
    public void receiveSticker(Sticker sticker) {
        if (sticker == null || context == null) return;

        // Phát âm thanh
        SoundManager.playSound("throw_sticker.mp3");

        // Tạo ImageView cho sticker
        ImageView stickerView = new ImageView();
        try {
            String imageUrl = sticker.getStickerPath();
            if (imageUrl != null && !imageUrl.isBlank()) {
                if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                    if (imageUrl.startsWith("/static/")) {
                        imageUrl = "http://localhost:8080" + imageUrl;
                    } else if (!imageUrl.startsWith("/")) {
                        imageUrl = "http://localhost:8080/static/sticker_match/" + imageUrl;
                    } else {
                        imageUrl = "http://localhost:8080" + imageUrl;
                    }
                }
                Image image = new Image(imageUrl, true);
                stickerView.setImage(image);
            }
        } catch (Exception e) {
            System.err.println("[MatchChat] Failed to load sticker image: " + e.getMessage());
            return;
        }

        stickerView.setFitWidth(70);
        stickerView.setFitHeight(70);
        stickerView.setPreserveRatio(true);

        // Đảm bảo animationLayer nằm trên cùng trong rightPane (StackPane)
        if (rightPane != null && rightAnimationLayer != null) {
            rightPane.getChildren().remove(rightAnimationLayer);
            rightPane.getChildren().add(rightAnimationLayer);
        }
        // Đảm bảo animation chạy sau khi layout đã render
        Platform.runLater(() -> {
            MatchStickerAnimator.animate(
                stickerView,
                rightAnimationLayer, // animationLayer
                rightPlayerAvatar, // startNode (hoặc rightPlayerName)
                rightMessageContainer, // endNode
                false, // isFromMe
                v -> {} // onFinish
            );
        });
    }
}
