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
                    previewLbl.setWrapText(true);
                    
                    selectedProperty().addListener((obs, wasSelected, isSelected) -> {
                        if (nameLbl != null && previewLbl != null) {
                            if (isSelected) {
                                nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0A4A6B; -fx-font-size: 14px;");
                                previewLbl.setStyle("-fx-text-fill: #1E7BA8; -fx-font-size: 11px; -fx-font-weight: 500;");
                            } else {
                                nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
                                previewLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
                            }
                        }
                    });
                }
                
                private javafx.scene.image.ImageView avatarView;
                private javafx.scene.shape.Circle avatarClip;
                
                {
                    avatarView = new javafx.scene.image.ImageView();
                    avatarView.setFitWidth(36);
                    avatarView.setFitHeight(36);
                    avatarView.setPreserveRatio(true);
                    avatarClip = new javafx.scene.shape.Circle(18, 18, 18);
                    avatarView.setClip(avatarClip);
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
                        return;
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
                    
                    if (lastMessageType != null && "STICKER".equalsIgnoreCase(lastMessageType.trim())) {
                        previewLbl.setText("📷 Sticker");
                    } else if (lastMessage == null || lastMessage.trim().isEmpty() || "null".equalsIgnoreCase(lastMessage.trim())) {
                        previewLbl.setText("No conversation yet");
                    } else {
                        previewLbl.setText(lastMessage);
                    }
                    
                    boolean selected = isSelected();
                    if (selected) {
                        nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #0A4A6B; -fx-font-size: 14px;");
                        previewLbl.setStyle("-fx-text-fill: #1E7BA8; -fx-font-size: 11px; -fx-font-weight: 500;");
                    } else {
                        nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");
                        previewLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
                    }

                    javafx.scene.layout.VBox textBox = new javafx.scene.layout.VBox(2, nameLbl, previewLbl);
                    javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10, avatarView, textBox);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    row.setPadding(new javafx.geometry.Insets(8, 10, 8, 10));

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
                    String relativeTime = TimestampUtil.getRelativeTime(conv.lastMessageTime);
                    items.add(new ConversationItem(
                        conv.otherUserId,
                        conv.otherUsername,
                        conv.otherDisplayName,
                        conv.otherAvatarUrl,
                        conv.lastMessageText,
                        String.valueOf(conv.lastMessageType),
                        relativeTime, // Use relative time instead of raw timestamp
                        conv.lastMessageTime // Keep original timestamp for updates
                    ));
                    
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
                "", null, "", null
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
                            
                            javafx.application.Platform.runLater(() -> {
                                if (currentContext == finalContext && statusLabel != null) {
                                    String statusText = mapStatus(userInfo.status);
                                    statusLabel.setText(statusText);
                                    statusLabel.getStyleClass().removeAll("friend-status", "status-online", "status-offline", "status-busy");
                                    statusLabel.getStyleClass().addAll("friend-status", getStatusTextClass(userInfo.status));
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
                String lastText = "";
                if (msg.getMessageType() == com.example.memorygame.model.chat.MessageType.STICKER) {
                    lastText = "📷 Sticker";
                } else {
                    lastText = msg.getContent() != null ? msg.getContent() : "";
                }
                
                // Chỉ cập nhật nếu message thay đổi
                if (lastText.equals(item.getLastMessageText())) {
                    return;
                }
                
                java.time.LocalDateTime msgTime = msg.getTimestamp() != null ? msg.getTimestamp() : java.time.LocalDateTime.now();
                String relativeTime = TimestampUtil.getRelativeTime(msgTime);
                ConversationItem updatedItem = new ConversationItem(
                    item.getUserId(),
                    item.getUsername(),
                    item.getDisplayName(),
                    item.getAvatarUrl(),
                    lastText,
                    String.valueOf(msg.getMessageType()),
                    relativeTime,
                    msgTime
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
            if (currentContext != null && currentContext.getOtherUser() != null 
                && currentContext.getOtherUser().id == userId) {
                currentContext.getOtherUser().status = finalStatus;
            }
            
            javafx.application.Platform.runLater(() -> {
                updateUserStatus(userId, finalStatus);
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

        public ConversationItem(long userId, String username, String displayName, String avatarUrl,
                                 String lastMessageText, String lastMessageType, String lastMessageTime) {
            this(userId, username, displayName, avatarUrl, lastMessageText, lastMessageType, lastMessageTime, null);
        }
        
        public ConversationItem(long userId, String username, String displayName, String avatarUrl,
                                 String lastMessageText, String lastMessageType, String lastMessageTime, 
                                 java.time.LocalDateTime lastMessageTimestamp) {
            this.userId = userId;
            this.username = username;
            this.displayName = displayName;
            this.avatarUrl = avatarUrl;
            this.lastMessageText = lastMessageText;
            this.lastMessageType = lastMessageType;
            this.lastMessageTime = lastMessageTime;
            this.lastMessageTimestamp = lastMessageTimestamp;
        }

        public long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getDisplayName() { return displayName; }
        public String getAvatarUrl() { return avatarUrl; }
        public String getLastMessageText() { return lastMessageText; }
        public String getLastMessageType() { return lastMessageType; }
        public String getLastMessageTime() { return lastMessageTime; }
        public java.time.LocalDateTime getLastMessageTimestamp() { return lastMessageTimestamp; }
        public String displayNameOrUsername() {
            return (displayName != null && !displayName.isBlank()) ? displayName : username;
        }
        
        /**
         * Tạo item mới với thời gian tương đối được cập nhật
         */
        public ConversationItem updateRelativeTime() {
            String newRelativeTime = TimestampUtil.getRelativeTime(lastMessageTimestamp);
            return new ConversationItem(userId, username, displayName, avatarUrl, 
                                        lastMessageText, lastMessageType, newRelativeTime, lastMessageTimestamp);
        }
    }
}
