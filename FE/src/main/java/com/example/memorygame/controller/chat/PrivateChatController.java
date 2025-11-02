package com.example.memorygame.controller.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.memorygame.controller.chat.contexts.PrivateChatContext;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.TCPClient;

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
    // Danh sách đầy đủ để filter theo search
    private final List<ConversationItem> allConversations = new ArrayList<>();

    public PrivateChatController() {
        this.tcpClient = TCPClient.getInstance();
    }

    @FXML
    private void initialize() {
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

        // Đăng ký handlers cho TCP client nếu chưa có
        tcpClient.registerChatHandlers();

        // Bind search filter (bên trái)
        if (searchField != null) {
            searchField.textProperty().addListener((obs, o, n) -> applyFilter(n));
        }
    }

    public void setCurrentUser(UserSummary currentUser) {
        this.currentUser = currentUser;
        loadConversations();
    }

    private void loadConversations() {
        if (conversationList == null || currentUser == null) return;

        conversationList.getItems().clear();

        // Custom cell: avatar + name + last message preview
        conversationList.setCellFactory(param -> new javafx.scene.control.ListCell<ConversationItem>() {
            @Override
            protected void updateItem(ConversationItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                javafx.scene.image.ImageView avatar = new javafx.scene.image.ImageView();
                avatar.setFitWidth(36); avatar.setFitHeight(36); avatar.setPreserveRatio(true);
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
                avatar.setClip(clip);
                try {
                    if (item.getAvatarUrl() != null && !item.getAvatarUrl().isBlank()) {
                        avatar.setImage(new javafx.scene.image.Image(item.getAvatarUrl(), true));
                    } else {
                        avatar.setImage(new javafx.scene.image.Image(
                            getClass().getResourceAsStream("/com/example/memorygame/assets/images/name.png")
                        ));
                    }
                } catch (Exception ignored) {}

                javafx.scene.control.Label nameLbl = new javafx.scene.control.Label(item.displayNameOrUsername());
                nameLbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #111827;");

                javafx.scene.control.Label previewLbl = new javafx.scene.control.Label(
                    item.getLastMessageText() == null ? "" : item.getLastMessageText()
                );
                previewLbl.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 11px;");
                previewLbl.setWrapText(true);

                javafx.scene.layout.VBox textBox = new javafx.scene.layout.VBox(2, nameLbl, previewLbl);
                javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10, avatar, textBox);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new javafx.geometry.Insets(8, 10, 8, 10));

                setText(null);
                setGraphic(row);
            }
        });

        // Fetch conversation list từ BE
        new Thread(() -> {
            try {
                List<com.example.memorygame.utils.ChatApi.ConversationPreview> conversations =
                    com.example.memorygame.utils.ChatApi.fetchConversationList(currentUser.id);

                List<ConversationItem> items = new ArrayList<>();
                for (com.example.memorygame.utils.ChatApi.ConversationPreview conv : conversations) {
                    items.add(new ConversationItem(
                        conv.otherUserId,
                        conv.otherUsername,
                        conv.otherDisplayName,
                        conv.otherAvatarUrl,
                        conv.lastMessageText,
                        String.valueOf(conv.lastMessageType),
                        String.valueOf(conv.lastMessageTime)
                    ));
                }

                javafx.application.Platform.runLater(() -> {
                    allConversations.clear();
                    allConversations.addAll(items);
                    conversationList.getItems().setAll(items);
                });
            } catch (Exception e) {
                System.err.println("[PrivateChatController] Failed to load conversations: " + e.getMessage());
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
                "", null, null
            );
            allConversations.add(0, item);
            conversationList.getItems().add(0, item);
        }
    }

    public void openConversation(UserSummary otherUser) {
        if (currentUser == null || otherUser == null) return;

        if (currentContext != null && currentChatComponent != null) {
            tcpClient.unsubscribeFromChannel(currentContext.getChannelId());
        }

        PrivateChatContext context = conversations.get(otherUser.id);
        if (context == null) {
            context = new PrivateChatContext(currentUser, otherUser);
            conversations.put(otherUser.id, context);
        }

        currentContext = context;
        currentChatComponent = new ChatComponent(context);

        if (chatContainer != null) {
            if (emptyState != null) { emptyState.setVisible(false); emptyState.setManaged(false); }
            chatContainer.getChildren().clear();
            chatContainer.getChildren().add(currentChatComponent);
        }

        if (chatTitle != null) chatTitle.setText(context.getTitle());
        if (statusLabel != null) statusLabel.setText(context.isOtherUserOnline() ? "🟢 online" : "⚫ offline");

        // Update header avatar
        if (chatAvatar != null) {
            try {
                javafx.scene.image.Image img;
                if (otherUser.avatarUrl != null && !otherUser.avatarUrl.isBlank()) {
                    img = new javafx.scene.image.Image(otherUser.avatarUrl, true);
                } else {
                    img = new javafx.scene.image.Image(
                        getClass().getResourceAsStream("/com/example/memorygame/assets/images/name.png")
                    );
                }
                chatAvatar.setImage(img);
                chatAvatar.setFitWidth(36);
                chatAvatar.setFitHeight(36);
                chatAvatar.setPreserveRatio(true);
                // circular clip
                javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(18, 18, 18);
                chatAvatar.setClip(clip);
            } catch (Exception ignored) {}
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

    public void updateUserStatus(long userId, String status) {
        // Optional: extend ConversationItem with status if needed
        if (statusLabel != null && currentContext != null && currentContext.getOtherUser().id == userId) {
            statusLabel.setText("online".equalsIgnoreCase(status) ? "🟢 online" : "⚫ offline");
        }
    }

    public void cleanup() {
        if (currentContext != null) tcpClient.unsubscribeFromChannel(currentContext.getChannelId());
        for (PrivateChatContext ctx : conversations.values()) tcpClient.unsubscribeFromChannel(ctx.getChannelId());
        conversations.clear();
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

    // Simple view model for conversation list item
    public static class ConversationItem {
        private final long userId;
        private final String username;
        private final String displayName;
        private final String avatarUrl;
        private final String lastMessageText;
        private final String lastMessageType;
        private final String lastMessageTime;

        public ConversationItem(long userId, String username, String displayName, String avatarUrl,
                                 String lastMessageText, String lastMessageType, String lastMessageTime) {
            this.userId = userId;
            this.username = username;
            this.displayName = displayName;
            this.avatarUrl = avatarUrl;
            this.lastMessageText = lastMessageText;
            this.lastMessageType = lastMessageType;
            this.lastMessageTime = lastMessageTime;
        }

        public long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getDisplayName() { return displayName; }
        public String getAvatarUrl() { return avatarUrl; }
        public String getLastMessageText() { return lastMessageText; }
        public String getLastMessageType() { return lastMessageType; }
        public String getLastMessageTime() { return lastMessageTime; }
        public String displayNameOrUsername() {
            return (displayName != null && !displayName.isBlank()) ? displayName : username;
        }
    }
}
