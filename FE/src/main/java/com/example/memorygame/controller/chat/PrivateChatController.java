package com.example.memorygame.controller.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.memorygame.controller.chat.contexts.PrivateChatContext;
import com.example.memorygame.model.chat.ChatMessage;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.TCPClient;
import com.example.memorygame.utils.TimestampUtil;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Controller cho PrivateChat.fxml
 */
public class PrivateChatController {

    @FXML private ListView<ConversationItem> conversationList;
    @FXML private VBox chatContainer;
    @FXML private VBox emptyState;
    @FXML private Label chatTitle;
    @FXML private Label statusLabel;
    @FXML private javafx.scene.image.ImageView chatAvatar;
    @FXML private TextField searchField;

    private ChatComponent currentChatComponent;
    private PrivateChatContext currentContext;
    private UserSummary currentUser;
    private final TCPClient tcpClient;

    // Cache các conversations đã mở
    private final Map<Long, PrivateChatContext> conversations = new HashMap<>();
    // Cache ChatComponent để tránh reload tin nhắn và avatar
    private final Map<Long, ChatComponent> chatComponentCache = new HashMap<>();
    // Danh sách đầy đủ để filter theo search
    private final List<ConversationItem> allConversations = new ArrayList<>();
    // Map lưu status của các user (được cập nhật từ TCP)
    private final Map<Long, String> userStatusMap = new HashMap<>();

    public PrivateChatController() {
        this.tcpClient = TCPClient.getInstance();
    }

    @FXML
    private void initialize() {
        if (emptyState != null) {
            emptyState.setVisible(false);
            emptyState.setManaged(false);
        }
        
        // Thiết lập background image cho chat container
        if (chatContainer != null) {
            try {
                java.net.URL bgUrl = getClass().getResource("/com/example/memorygame/assets/images/bg_2.png");
                if (bgUrl != null) {
                    String bgStyle = String.format(
                        "-fx-background-image: url('%s'); -fx-background-size: cover; -fx-background-repeat: no-repeat; -fx-background-position: center center;",
                        bgUrl.toExternalForm()
                    );
                    chatContainer.setStyle(bgStyle);
                }
            } catch (Exception e) {
                System.err.println("[PrivateChat] Không thể tải background image: " + e.getMessage());
            }
        }
        
        // Xử lý chọn conversation từ list
        if (conversationList != null) {
            conversationList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    UserSummary other = new UserSummary();
                    other.id = newVal.getUserId();
                    other.username = newVal.getUsername();
                    other.displayName = newVal.getDisplayName();
                    other.avatarUrl = newVal.getAvatarUrl();
                    openConversation(other);
                }
            });
        }

        // Đăng ký handlers cho TCP client
        tcpClient.registerChatHandlers();
        tcpClient.onMessage("USER_STATUS", message -> {
            handleUserStatusChange(message);
        });

        // Lọc conversation theo search
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyFilter(n));
        }
        
        // Cập nhật thời gian tương đối mỗi phút
        setupRelativeTimeUpdater();
    }
    
    /**
     * Thiết lập timer để cập nhật thời gian tương đối trong danh sách conversation
     */
    private void setupRelativeTimeUpdater() {
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.minutes(1), e -> updateConversationTimes())
        );
        timeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        timeline.play();
    }
    
    /**
     * Cập nhật thời gian tương đối cho tất cả conversations
     */
    private void updateConversationTimes() {
        if (conversationList == null) return;
        
        javafx.application.Platform.runLater(() -> {
            var items = conversationList.getItems();
            for (int i = 0; i < items.size(); i++) {
                ConversationItem oldItem = items.get(i);
                if (oldItem.getLastMessageTimestamp() != null) {
                    ConversationItem newItem = oldItem.updateRelativeTime();
                    items.set(i, newItem);
                }
            }
            
            for (int i = 0; i < allConversations.size(); i++) {
                ConversationItem oldItem = allConversations.get(i);
                if (oldItem.getLastMessageTimestamp() != null) {
                    allConversations.set(i, oldItem.updateRelativeTime());
                }
            }
            
            // Refresh list view để cell được render lại với relative time mới
            conversationList.refresh();
        });
    }

    public void setCurrentUser(UserSummary currentUser) {
        this.currentUser = currentUser;
        loadConversations();
    }

    private void loadConversations() {
        if (conversationList == null || currentUser == null) return;

        // Thiết lập cell factory một lần
        if (conversationList.getCellFactory() == null) {
            conversationList.setCellFactory(param -> {
            javafx.scene.control.ListCell<ConversationItem> cell = new javafx.scene.control.ListCell<ConversationItem>() {
                private javafx.scene.control.Label nameLbl;
                private javafx.scene.control.Label previewLbl;
                
                {
                    nameLbl = new javafx.scene.control.Label();
                    nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
                    
                    previewLbl = new javafx.scene.control.Label();
                    previewLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
                    previewLbl.setWrapText(false); // Không wrap để tránh cell mở rộng
                    previewLbl.setMaxWidth(Double.MAX_VALUE); // Cho phép truncate
                    previewLbl.setEllipsisString("..."); // Thêm ellipsis khi text quá dài
                    
                    selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                        updateCellStyle(isSelected);
                    });
                }
                
                // Helper method để cập nhật style
                private void updateCellStyle(boolean isSelected) {
                    if (nameLbl != null && previewLbl != null) {
                        if (isSelected) {
                            // Background đậm hơn cho cell
                            setStyle("-fx-background-color: #E3F2FD; -fx-background-radius: 4px;");
                            nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0A4A6B; -fx-font-size: 14px;");
                            previewLbl.setStyle("-fx-text-fill: #1E7BA8; -fx-font-size: 11px; -fx-font-weight: bold;");
                        } else {
                            // Background trong suốt
                            setStyle("-fx-background-color: transparent;");
                            nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
                            previewLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px; -fx-font-weight: normal;");
                        }
                    }
                }
                
                private javafx.scene.image.ImageView avatarView;
                private javafx.scene.shape.Circle avatarClip;
                private javafx.scene.layout.StackPane avatarContainer;
                private javafx.scene.shape.Circle statusDot;
                
                {
                    avatarView = new javafx.scene.image.ImageView();
                    avatarView.setFitWidth(36);
                    avatarView.setFitHeight(36);
                    avatarView.setPreserveRatio(true);
                    avatarClip = new javafx.scene.shape.Circle(18, 18, 18);
                    avatarView.setClip(avatarClip);
                    
                    // Status dot
                    statusDot = new javafx.scene.shape.Circle(5);
                    statusDot.getStyleClass().add("status-offline");
                    
                    // Avatar container với status dot
                    avatarContainer = new javafx.scene.layout.StackPane();
                    avatarContainer.getChildren().addAll(avatarView, statusDot);
                    javafx.scene.layout.StackPane.setAlignment(statusDot, javafx.geometry.Pos.BOTTOM_RIGHT);
                    javafx.scene.layout.StackPane.setMargin(statusDot, new javafx.geometry.Insets(0, 0, 2, 0));
                }
                
                @Override
                protected void updateItem(ConversationItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        if (avatarView != null) {
                            avatarView.setImage(null);
                        }
                        if (statusDot != null) {
                            statusDot.setVisible(false);
                        }
                        return;
                    }
                    
                    // Hiển thị status dot - lấy từ userStatusMap
                    if (statusDot != null) {
                        String status = item.getStatus();
                        // Nếu chưa có trong item, lấy từ userStatusMap
                        if (status == null) {
                            status = userStatusMap.get(item.getUserId());
                        }
                        String statusClass = getStatusDotClass(status);
                        statusDot.getStyleClass().clear();
                        statusDot.getStyleClass().add(statusClass);
                        statusDot.setVisible(true);
                    }

                    // Sử dụng avatar cache để tối ưu hiệu suất
                    if (avatarView != null) {
                        try {
                            com.example.memorygame.utils.AvatarCacheManager cacheManager = 
                                com.example.memorygame.utils.AvatarCacheManager.getInstance();
                            
                            String avatarUrl = item.getAvatarUrl();
                            final javafx.scene.image.ImageView finalAvatarView = avatarView;
                            javafx.scene.image.Image image = cacheManager.getAvatarWithCallback(avatarUrl, () -> {
                                javafx.scene.image.Image loadedImage = cacheManager.getAvatar(avatarUrl);
                                if (loadedImage != null && loadedImage != cacheManager.getDefaultAvatar()) {
                                    finalAvatarView.setImage(loadedImage);
                                }
                            });
                            
                            if (image != null && avatarView.getImage() != image) {
                                avatarView.setImage(image);
                            }
                        } catch (Exception e) {
                            System.err.println("[ConversationCell] Lỗi tải avatar: " + e.getMessage());
                        }
                    }

                    nameLbl.setText(item.displayNameOrUsername());
                    
                    // Xử lý preview tin nhắn cuối
                    String lastMessageType = item.getLastMessageType();
                    String lastMessage = item.getLastMessageText();
                    String messageText = "";
                    
                    if (lastMessageType != null && "STICKER".equalsIgnoreCase(lastMessageType.trim())) {
                        messageText = "📷 Sticker";
                    } else if (lastMessage == null || lastMessage.trim().isEmpty() || "null".equalsIgnoreCase(lastMessage.trim())) {
                        messageText = "No conversation yet";
                    } else {
                        messageText = lastMessage;
                    }
                    
                    // Lấy relative time trước để tính toán độ dài
                    String relativeTime = "";
                    if (!messageText.equals("No conversation yet")) {
                        java.time.LocalDateTime timestamp = item.getLastMessageTimestamp();
                        if (timestamp != null) {
                            try {
                                relativeTime = TimestampUtil.getRelativeTimeForConversation(timestamp);
                                if (relativeTime == null || relativeTime.isEmpty()) {
                                    relativeTime = "";
                                }
                            } catch (Exception e) {
                                System.err.println("[ConversationCell] Lỗi tính relative time: " + e.getMessage());
                                e.printStackTrace();
                            }
                        }
                    }
                    
                    // Tạo preview text với relative time và truncate
                    String previewText = messageText;
                    if (!relativeTime.isEmpty() && !messageText.equals("No conversation yet")) {
                        String fullText = messageText + " · " + relativeTime;
                        
                        // Truncate nếu quá dài (ước tính ~35-40 ký tự cho cell width, trừ đi phần " · X phút trước")
                        int maxMessageLength = 35 - relativeTime.length() - 3; // 3 cho " · "
                        if (maxMessageLength < 5) maxMessageLength = 5; // Tối thiểu 5 ký tự
                        
                        if (messageText.length() > maxMessageLength) {
                            messageText = messageText.substring(0, maxMessageLength - 3) + "...";
                            previewText = messageText + " · " + relativeTime;
                        } else {
                            previewText = fullText;
                        }
                    } else if (!messageText.equals("No conversation yet") && messageText.length() > 35) {
                        // Truncate ngay cả khi không có relative time
                        previewText = messageText.substring(0, 32) + "...";
                    }
                    
                    previewLbl.setText(previewText);
                    
                    // Cập nhật style dựa trên selected state
                    updateCellStyle(isSelected());

                    javafx.scene.layout.VBox textBox = new javafx.scene.layout.VBox(2, nameLbl, previewLbl);
                    textBox.setMaxWidth(Double.MAX_VALUE); // Cho phép truncate
                    javafx.scene.layout.HBox.setHgrow(textBox, javafx.scene.layout.Priority.ALWAYS);
                    
                    javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10, avatarContainer, textBox);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setPadding(new javafx.geometry.Insets(8, 10, 8, 10));
                    row.setMaxWidth(Double.MAX_VALUE); // Đảm bảo không mở rộng quá

                    setText(null);
                    setGraphic(row);
                }
            };
            return cell;
        });
        }

        javafx.application.Platform.runLater(() -> {
            if (emptyState != null) {
                emptyState.setVisible(false);
                emptyState.setManaged(false);
            }
        });
        
        new Thread(() -> {
            try {
                List<com.example.memorygame.utils.ChatApi.ConversationPreview> conversations =
                    com.example.memorygame.utils.ChatApi.fetchFriendsWithConversations(currentUser.id);

                List<ConversationItem> items = new ArrayList<>();
                com.example.memorygame.utils.AvatarCacheManager cacheManager = 
                    com.example.memorygame.utils.AvatarCacheManager.getInstance();
                
                for (com.example.memorygame.utils.ChatApi.ConversationPreview conv : conversations) {
                    String relativeTime = conv.lastMessageTime != null 
                        ? TimestampUtil.getRelativeTimeForConversation(conv.lastMessageTime) 
                        : "";
                    
                    if (conv.lastMessageTime == null) {
                        System.err.println("[PrivateChatController] WARNING: lastMessageTime is null for user " + conv.otherUserId + 
                            ", lastMessageText: " + conv.lastMessageText);
                    }
                    
                    // Lấy status từ userStatusMap (đã được cập nhật từ TCP hoặc context)
                    String userStatus = userStatusMap.get(conv.otherUserId);
                    
                    ConversationItem item = new ConversationItem(
                        conv.otherUserId,
                        conv.otherUsername,
                        conv.otherDisplayName,
                        conv.otherAvatarUrl,
                        conv.lastMessageText,
                        String.valueOf(conv.lastMessageType),
                        relativeTime, // Use relative time instead of raw timestamp
                        conv.lastMessageTime, // Keep original timestamp for updates (can be null)
                        userStatus // Status của user
                    );
                    
                    items.add(item);
                    
                    // Preload avatar for faster display
                    if (conv.otherAvatarUrl != null) {
                        cacheManager.preloadAvatar(conv.otherAvatarUrl);
                    }
                }

                javafx.application.Platform.runLater(() -> {
                    Long currentOpenUserId = currentContext != null && currentContext.getOtherUser() != null 
                        ? currentContext.getOtherUser().id : null;
                    
                    allConversations.clear();
                    allConversations.addAll(items);
                    
                    // Cập nhật danh sách conversation
                    for (ConversationItem newItem : items) {
                        boolean found = false;
                        for (int i = 0; i < conversationList.getItems().size(); i++) {
                            ConversationItem existingItem = conversationList.getItems().get(i);
                            if (existingItem.getUserId() == newItem.getUserId()) {
                                if (!existingItem.getLastMessageText().equals(newItem.getLastMessageText()) ||
                                    !existingItem.getLastMessageType().equals(newItem.getLastMessageType())) {
                                    conversationList.getItems().set(i, newItem);
                                }
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            conversationList.getItems().add(newItem);
                        }
                    }
                    
                    conversationList.getItems().removeIf(item -> 
                        items.stream().noneMatch(newItem -> newItem.getUserId() == item.getUserId())
                    );
                    
                    // Sắp xếp theo thứ tự mới (mới nhất ở đầu)
                    conversationList.getItems().sort((a, b) -> {
                        int indexA = items.indexOf(items.stream()
                            .filter(item -> item.getUserId() == a.getUserId())
                            .findFirst().orElse(null));
                        int indexB = items.indexOf(items.stream()
                            .filter(item -> item.getUserId() == b.getUserId())
                            .findFirst().orElse(null));
                        return Integer.compare(indexA >= 0 ? indexA : Integer.MAX_VALUE, 
                                             indexB >= 0 ? indexB : Integer.MAX_VALUE);
                    });
                    
                    // Khôi phục conversation đang mở
                    if (currentOpenUserId != null) {
                        for (ConversationItem item : conversationList.getItems()) {
                            if (item.getUserId() == currentOpenUserId) {
                                if (currentContext == null || currentContext.getOtherUser() == null || 
                                    currentContext.getOtherUser().id != currentOpenUserId) {
                                    UserSummary other = new UserSummary();
                                    other.id = item.getUserId();
                                    other.username = item.getUsername();
                                    other.displayName = item.getDisplayName();
                                    other.avatarUrl = item.getAvatarUrl();
                                    openConversation(other);
                                }
                                conversationList.getSelectionModel().select(item);
                                break;
                            }
                        }
                    } else if (!items.isEmpty() && currentContext == null) {
                        ConversationItem firstItem = items.get(0);
                        UserSummary other = new UserSummary();
                        other.id = firstItem.getUserId();
                        other.username = firstItem.getUsername();
                        other.displayName = firstItem.getDisplayName();
                        other.avatarUrl = firstItem.getAvatarUrl();
                        openConversation(other);
                        conversationList.getSelectionModel().select(firstItem);
                    } else if (items.isEmpty()) {
                        if (emptyState != null) {
                            emptyState.setVisible(true);
                            emptyState.setManaged(true);
                        }
                        if (currentContext != null) {
                            currentContext = null;
                            currentChatComponent = null;
                            if (chatContainer != null) {
                                chatContainer.getChildren().clear();
                            }
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("[PrivateChatController] Không thể tải danh sách conversation: " + e.getMessage());
                e.printStackTrace();
                javafx.application.Platform.runLater(() -> {
                    if (emptyState != null) {
                        emptyState.setVisible(true);
                        emptyState.setManaged(true);
                    }
                });
            }
        }).start();
    }

    private void applyFilter(String query) {
        if (conversationList == null) return;
        String q = query == null ? "" : query.trim().toLowerCase();
        if (q.isEmpty()) {
            conversationList.getItems().setAll(allConversations);
            return;
        }
        List<ConversationItem> filtered = new ArrayList<>();
        for (ConversationItem i : allConversations) {
            String name = i.displayNameOrUsername().toLowerCase();
            String preview = i.getLastMessageText() == null ? "" : i.getLastMessageText().toLowerCase();
            if (name.contains(q) || preview.contains(q)) filtered.add(i);
        }
        conversationList.getItems().setAll(filtered);
    }

    public void addConversation(UserSummary otherUser) {
        if (otherUser == null || conversationList == null) return;
        boolean exists = allConversations.stream().anyMatch(u -> u.getUserId() == otherUser.id);
        if (!exists) {
            ConversationItem item = new ConversationItem(
                otherUser.id, otherUser.username, otherUser.displayName, otherUser.avatarUrl,
                "", null, "", null, otherUser.status
            );
            allConversations.add(0, item);
            conversationList.getItems().add(0, item);
        }
    }

    public void openConversation(UserSummary otherUser) {
        if (currentUser == null || otherUser == null) return;

        PrivateChatContext oldContext = currentContext;
        Long oldOtherUserId = oldContext != null && oldContext.getOtherUser() != null 
            ? oldContext.getOtherUser().id : null;
        
        // Nếu conversation đã mở, không cần reload
        if (oldOtherUserId != null && oldOtherUserId == otherUser.id && currentChatComponent != null) {
            return;
        }

        // Giữ subscriptions cho tất cả conversations để nhận tin nhắn real-time
        PrivateChatContext context = conversations.get(otherUser.id);
        if (context == null) {
            context = new PrivateChatContext(currentUser, otherUser);
            conversations.put(otherUser.id, context);
            // Đăng ký nhận tin nhắn real-time cho channel này
            tcpClient.subscribeToChannel(context.getChannelId(), msg -> {
                javafx.application.Platform.runLater(() -> {
                    if (currentContext != null && currentContext.getChannelId().equals(msg.getChannelId())) {
                        if (currentChatComponent != null) {
                            currentChatComponent.addMessage(msg);
                        }
                    }
                    updateConversationLastMessage(msg);
                });
            });
        }

        currentContext = context;
        
        // Lưu status vào userStatusMap
        if (otherUser.status != null) {
            userStatusMap.put(otherUser.id, otherUser.status);
        }
        
        // Sử dụng cache ChatComponent để tránh reload
        currentChatComponent = chatComponentCache.get(otherUser.id);
        if (currentChatComponent == null) {
            currentChatComponent = new ChatComponent(context, msg -> {
                updateConversationLastMessage(msg);
            });
            chatComponentCache.put(otherUser.id, currentChatComponent);
        } else {
            currentChatComponent.setOnMessageSentCallback(msg -> {
                updateConversationLastMessage(msg);
            });
        }

        if (chatContainer != null) {
            if (emptyState != null) { emptyState.setVisible(false); emptyState.setManaged(false); }
            chatContainer.getChildren().clear();
            chatContainer.getChildren().add(currentChatComponent);
        }

        if (chatTitle != null) chatTitle.setText(context.getTitle());
        
        // Chọn conversation tương ứng trong list
        if (conversationList != null) {
            ConversationItem currentlySelected = conversationList.getSelectionModel().getSelectedItem();
            if (currentlySelected == null || currentlySelected.getUserId() != otherUser.id) {
                javafx.application.Platform.runLater(() -> {
                    for (ConversationItem item : conversationList.getItems()) {
                        if (item.getUserId() == otherUser.id) {
                            conversationList.getSelectionModel().select(item);
                            conversationList.scrollTo(item);
                            break;
                        }
                    }
                });
            }
        }
        
        // Hiển thị status của user
        final PrivateChatContext finalContext = context;
        if (statusLabel != null && otherUser != null && otherUser.id > 0) {
            if (otherUser.status != null && !otherUser.status.trim().isEmpty()) {
                // Lưu status vào userStatusMap để dùng cho conversation list
                userStatusMap.put(otherUser.id, otherUser.status);
                
                String statusText = mapStatus(otherUser.status);
                statusLabel.setText(statusText);
                statusLabel.getStyleClass().removeAll("friend-status", "status-online", "status-offline", "status-busy");
                statusLabel.getStyleClass().addAll("friend-status", getStatusTextClass(otherUser.status));
            } else {
                statusLabel.setText("Offline");
                statusLabel.getStyleClass().removeAll("friend-status", "status-online", "status-offline", "status-busy");
                statusLabel.getStyleClass().addAll("friend-status", "status-offline");
                
                // Lấy status từ API nếu chưa có
                new Thread(() -> {
                    try {
                        com.example.memorygame.model.user.UserSummary userInfo = 
                            com.example.memorygame.utils.UserApi.getUserById(otherUser.id);
                        if (userInfo != null && userInfo.status != null) {
                            finalContext.getOtherUser().status = userInfo.status;
                            // Lưu vào userStatusMap để dùng cho conversation list
                            userStatusMap.put(otherUser.id, userInfo.status);
                            
                            javafx.application.Platform.runLater(() -> {
                                if (currentContext == finalContext && statusLabel != null) {
                                    String statusText = mapStatus(userInfo.status);
                                    statusLabel.setText(statusText);
                                    statusLabel.getStyleClass().removeAll("friend-status", "status-online", "status-offline", "status-busy");
                                    statusLabel.getStyleClass().addAll("friend-status", getStatusTextClass(userInfo.status));
                                }
                                // Refresh conversation list để hiển thị status dot
                                if (conversationList != null) {
                                    conversationList.refresh();
                                }
                            });
                        }
                    } catch (Exception e) {
                        System.err.println("[PrivateChat] Không thể lấy user status: " + e.getMessage());
                    }
                }).start();
            }
        }

        // Cập nhật avatar trong header
        if (chatAvatar != null) {
            try {
                javafx.scene.image.Image img;
                if (otherUser.avatarUrl != null && !otherUser.avatarUrl.isBlank()) {
                    String imageUrl = otherUser.avatarUrl;
                    if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
                        if (imageUrl.startsWith("/static/")) {
                            imageUrl = "http://localhost:8080" + imageUrl;
                        } else if (!imageUrl.startsWith("/")) {
                            imageUrl = "http://localhost:8080/static/avatars/" + imageUrl;
                        } else {
                            imageUrl = "http://localhost:8080" + imageUrl;
                        }
                    }
                    img = new javafx.scene.image.Image(imageUrl, true);
                } else {
                    img = new javafx.scene.image.Image(
                        "http://localhost:8080/static/avatars/default_avatar.png", true
                    );
                }
                chatAvatar.setImage(img);
                chatAvatar.setFitWidth(36);
                chatAvatar.setFitHeight(36);
                chatAvatar.setPreserveRatio(true);
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
                chatAvatar.setClip(clip);
            } catch (Exception ignored) {}
        }
    }

    /**
     * Cập nhật tin nhắn cuối trong danh sách conversation khi nhận/gửi tin nhắn real-time
     */
    private void updateConversationLastMessage(ChatMessage msg) {
        if (msg == null || msg.getType() != com.example.memorygame.model.chat.ChatType.PRIVATE || currentUser == null) return;
        
        // Xác định user ID của người kia từ message
        Long otherUserId = null;
        
        if (msg.getSender() != null && msg.getSender().id == currentUser.id && msg.getReceiver() != null) {
            otherUserId = msg.getReceiver().id;
        } else if (msg.getSender() != null && msg.getSender().id != currentUser.id) {
            otherUserId = msg.getSender().id;
        } else if (msg.getReceiver() != null && msg.getReceiver().id != currentUser.id) {
            otherUserId = msg.getReceiver().id;
        }
        
        if (otherUserId == null) return;
        
        // Tìm và cập nhật conversation item
        for (ConversationItem item : allConversations) {
            if (item.getUserId() == otherUserId) {
                // Lấy lastMessage từ message (giống như logic hiện tại)
                String lastText = "";
                if (msg.getMessageType() == com.example.memorygame.model.chat.MessageType.STICKER) {
                    lastText = "📷 Sticker";
                } else {
                    lastText = msg.getContent() != null ? msg.getContent() : "";
                }
                
                // Lấy timestamp từ message (hoặc now() nếu null) - cùng logic đơn giản như lastMessage
                java.time.LocalDateTime msgTime = msg.getTimestamp();
                if (msgTime == null) {
                    msgTime = java.time.LocalDateTime.now();
                }
                
                // Kiểm tra xem có thay đổi không (text hoặc timestamp mới hơn)
                java.time.LocalDateTime oldTimestamp = item.getLastMessageTimestamp();
                boolean textChanged = !lastText.equals(item.getLastMessageText());
                boolean timestampNewer = oldTimestamp == null || msgTime.isAfter(oldTimestamp);
                
                // Nếu không có gì thay đổi, không cần cập nhật
                if (!textChanged && !timestampNewer) {
                    return;
                }
                
                String relativeTime = TimestampUtil.getRelativeTimeForConversation(msgTime);
                ConversationItem updatedItem = new ConversationItem(
                    item.getUserId(),
                    item.getUsername(),
                    item.getDisplayName(),
                    item.getAvatarUrl(),
                    lastText,
                    String.valueOf(msg.getMessageType()),
                    relativeTime,
                    msgTime,
                    item.getStatus() // Giữ nguyên status
                );
                
                int index = allConversations.indexOf(item);
                if (index >= 0) {
                    allConversations.remove(index);
                    allConversations.add(0, updatedItem);
                } else {
                    allConversations.add(0, updatedItem);
                }
                
                // Cập nhật UI
                javafx.application.Platform.runLater(() -> {
                    if (conversationList != null) {
                        ConversationItem existingListItem = null;
                        int listIndex = -1;
                        for (int i = 0; i < conversationList.getItems().size(); i++) {
                            ConversationItem listItem = conversationList.getItems().get(i);
                            if (listItem.getUserId() == item.getUserId()) {
                                existingListItem = listItem;
                                listIndex = i;
                                break;
                            }
                        }
                        
                        Long currentOpenUserId = currentContext != null && currentContext.getOtherUser() != null 
                            ? currentContext.getOtherUser().id : null;
                        
                        if (existingListItem != null && listIndex >= 0) {
                            conversationList.getItems().set(listIndex, updatedItem);
                            
                            if (listIndex > 0) {
                                conversationList.getItems().remove(listIndex);
                                conversationList.getItems().add(0, updatedItem);
                            }
                        } else {
                            conversationList.getItems().add(0, updatedItem);
                        }
                        
                        // Refresh để cell render lại với relative time mới
                        conversationList.refresh();
                        
                        if (currentOpenUserId != null && currentOpenUserId == updatedItem.getUserId()) {
                            if (conversationList.getItems().size() > 0 && 
                                conversationList.getItems().get(0).getUserId() == updatedItem.getUserId()) {
                                conversationList.getSelectionModel().select(0);
                            }
                        }
                    }
                });
                break;
            }
        }
    }

    public void startConversation(UserSummary otherUser) {
        if (otherUser == null) return;
        addConversation(otherUser);
        openConversation(otherUser);
        if (conversationList != null) {
            for (ConversationItem i : conversationList.getItems()) {
                if (i.getUserId() == otherUser.id) {
                    conversationList.getSelectionModel().select(i);
                    break;
                }
            }
        }
    }

    /**
     * Xử lý thay đổi trạng thái user từ TCP
     */
    private void handleUserStatusChange(TCPClient.TCPMessage message) {
        Map<String, Object> data = message.getData();
        if (data == null) return;

        Object userObj = data.get("user");
        Object onlineObj = data.get("online");

        if (userObj == null || onlineObj == null) return;

        String username = userObj.toString();
        boolean online = Boolean.parseBoolean(onlineObj.toString());
        String status = online ? "ONLINE" : "OFFLINE";
        
        // Tìm userId từ username
        Long foundUserId = null;
        
        if (currentContext != null && currentContext.getOtherUser() != null) {
            if (username.equals(currentContext.getOtherUser().username)) {
                foundUserId = currentContext.getOtherUser().id;
            }
        }
        
        if (foundUserId == null) {
            for (ConversationItem item : allConversations) {
                if (username.equals(item.getUsername())) {
                    foundUserId = item.getUserId();
                    break;
                }
            }
        }
        
        final Long userId = foundUserId;
        final String finalStatus = status;
        if (userId != null) {
            // Lưu status vào map
            userStatusMap.put(userId, finalStatus);
            
            if (currentContext != null && currentContext.getOtherUser() != null 
                && currentContext.getOtherUser().id == userId) {
                currentContext.getOtherUser().status = finalStatus;
            }
            
            javafx.application.Platform.runLater(() -> {
                updateUserStatus(userId, finalStatus);
                
                // Cập nhật ConversationItem trong allConversations với status mới
                for (int i = 0; i < allConversations.size(); i++) {
                    ConversationItem item = allConversations.get(i);
                    if (item.getUserId() == userId) {
                        // Tạo ConversationItem mới với status được cập nhật
                        ConversationItem updatedItem = new ConversationItem(
                            item.getUserId(),
                            item.getUsername(),
                            item.getDisplayName(),
                            item.getAvatarUrl(),
                            item.getLastMessageText(),
                            item.getLastMessageType(),
                            item.getLastMessageTime(),
                            item.getLastMessageTimestamp(),
                            finalStatus // Cập nhật status mới
                        );
                        allConversations.set(i, updatedItem);
                        
                        // Cập nhật trong conversationList nếu có
                        if (conversationList != null) {
                            for (int j = 0; j < conversationList.getItems().size(); j++) {
                                ConversationItem listItem = conversationList.getItems().get(j);
                                if (listItem.getUserId() == userId) {
                                    conversationList.getItems().set(j, updatedItem);
                                    break;
                                }
                            }
                        }
                        break;
                    }
                }
                
                // Refresh conversation list để hiển thị status dot mới
                if (conversationList != null) {
                    conversationList.refresh();
                }
            });
        }
    }
    
    public void updateUserStatus(long userId, String status) {
        if (statusLabel != null && currentContext != null && currentContext.getOtherUser().id == userId) {
            String statusText = mapStatus(status);
            statusLabel.setText(statusText);
            statusLabel.getStyleClass().removeAll("friend-status", "status-online", "status-offline", "status-busy");
            statusLabel.getStyleClass().addAll("friend-status", getStatusTextClass(status));
        }
    }
    
    /**
     * Chuyển đổi status từ backend sang text hiển thị
     */
    private String mapStatus(String status) {
        if (status == null)
            return "Offline";
        String s = status.trim().toUpperCase();
        return switch (s) {
            case "ONLINE" -> "Online";
            case "BUSY", "IN_GAME" -> "Busy";
            default -> "Offline";
        };
    }
    
    /**
     * Lấy style class cho status text
     */
    private String getStatusTextClass(String status) {
        if (status == null)
            return "status-offline";
        String s = status.trim().toUpperCase();
        return switch (s) {
            case "ONLINE" -> "status-online";
            case "BUSY", "IN_GAME" -> "status-busy";
            default -> "status-offline";
        };
    }
    
    /**
     * Lấy style class cho status dot (Circle)
     */
    private String getStatusDotClass(String status) {
        if (status == null)
            return "status-offline";
        String s = status.trim().toUpperCase();
        return switch (s) {
            case "ONLINE" -> "status-online";
            case "BUSY", "IN_GAME" -> "status-busy";
            default -> "status-offline";
        };
    }

    public void cleanup() {
        if (currentContext != null) tcpClient.unsubscribeFromChannel(currentContext.getChannelId());
        for (PrivateChatContext ctx : conversations.values()) tcpClient.unsubscribeFromChannel(ctx.getChannelId());
        conversations.clear();
        chatComponentCache.clear();
        if (chatContainer != null) chatContainer.getChildren().clear();
        if (conversationList != null) conversationList.getItems().clear();
    }

    public PrivateChatContext getCurrentContext() { return currentContext; }

    public List<UserSummary> getConversations() {
        List<UserSummary> users = new ArrayList<>();
        for (ConversationItem i : (conversationList == null ? allConversations : conversationList.getItems())) {
            UserSummary u = new UserSummary();
            u.id = i.getUserId();
            u.username = i.getUsername();
            u.displayName = i.getDisplayName();
            u.avatarUrl = i.getAvatarUrl();
            users.add(u);
        }
        return users;
    }

    /**
     * View model cho item trong danh sách conversation
     */
    public static class ConversationItem {
        private final long userId;
        private final String username;
        private final String displayName;
        private final String avatarUrl;
        private final String lastMessageText;
        private final String lastMessageType;
        private final String lastMessageTime;
        private final java.time.LocalDateTime lastMessageTimestamp;
        private final String status;

        public ConversationItem(long userId, String username, String displayName, String avatarUrl,
                                 String lastMessageText, String lastMessageType, String lastMessageTime) {
            this(userId, username, displayName, avatarUrl, lastMessageText, lastMessageType, lastMessageTime, null, null);
        }
        
        public ConversationItem(long userId, String username, String displayName, String avatarUrl,
                                 String lastMessageText, String lastMessageType, String lastMessageTime, 
                                 java.time.LocalDateTime lastMessageTimestamp) {
            this(userId, username, displayName, avatarUrl, lastMessageText, lastMessageType, lastMessageTime, lastMessageTimestamp, null);
        }
        
        public ConversationItem(long userId, String username, String displayName, String avatarUrl,
                                 String lastMessageText, String lastMessageType, String lastMessageTime, 
                                 java.time.LocalDateTime lastMessageTimestamp, String status) {
            this.userId = userId;
            this.username = username;
            this.displayName = displayName;
            this.avatarUrl = avatarUrl;
            this.lastMessageText = lastMessageText;
            this.lastMessageType = lastMessageType;
            this.lastMessageTime = lastMessageTime;
            this.lastMessageTimestamp = lastMessageTimestamp;
            this.status = status;
        }

        public long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getDisplayName() { return displayName; }
        public String getAvatarUrl() { return avatarUrl; }
        public String getLastMessageText() { return lastMessageText; }
        public String getLastMessageType() { return lastMessageType; }
        public String getLastMessageTime() { return lastMessageTime; }
        public java.time.LocalDateTime getLastMessageTimestamp() { return lastMessageTimestamp; }
        public String getStatus() { return status; }
        public String displayNameOrUsername() {
            return (displayName != null && !displayName.isBlank()) ? displayName : username;
        }
        
        /**
         * Tạo item mới với thời gian tương đối được cập nhật
         */
        public ConversationItem updateRelativeTime() {
            String newRelativeTime = TimestampUtil.getRelativeTimeForConversation(lastMessageTimestamp);
            return new ConversationItem(userId, username, displayName, avatarUrl, 
                                        lastMessageText, lastMessageType, newRelativeTime, lastMessageTimestamp, status);
        }
    }
}
