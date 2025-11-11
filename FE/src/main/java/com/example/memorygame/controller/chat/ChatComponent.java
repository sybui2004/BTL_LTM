package com.example.memorygame.controller.chat;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.example.memorygame.model.chat.ChatMessage;
import com.example.memorygame.model.chat.MessageType;
import com.example.memorygame.model.chat.Sticker;
import com.example.memorygame.utils.SoundManager;
import com.example.memorygame.utils.TCPClient;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;

/**
 * Reusable Chat UI component. Loads base_chat.fxml and connects to TCPClient.
 * This implementation is intentionally minimal (simple message rendering) so
 * it can be extended later with richer UI elements.
 */
public class ChatComponent extends VBox {
    @FXML private VBox messageContainer;
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private Button emojiButton;
    @FXML private HBox inputArea;
    @FXML private javafx.scene.control.ScrollPane scrollPane; // Thêm dòng này
    
    private final ChatContext context;
    private final TCPClient tcpClient;
    private Popup stickerPopup;
    private StickerPicker stickerPicker;
    private java.util.function.Consumer<ChatMessage> onMessageSentCallback;
    
    // List to manage all messages in chronological order
    private final java.util.List<ChatMessage> messagesList = new java.util.ArrayList<>();
    
    public ChatComponent(ChatContext context) {
        this(context, null);
    }

    public ChatComponent(ChatContext context, java.util.function.Consumer<ChatMessage> onMessageSentCallback) {
        this.context = context;
        this.tcpClient = TCPClient.getInstance();
        this.onMessageSentCallback = onMessageSentCallback;
        loadFXML();
        setupUI();
        setupMessageHandling();
        loadHistoryIfAvailable();
    }
    
    /**
     * Set the callback for when a message is sent (used for cached components)
     */
    public void setOnMessageSentCallback(java.util.function.Consumer<ChatMessage> onMessageSentCallback) {
        this.onMessageSentCallback = onMessageSentCallback;
    }
    
    /**
     * Load history messages if context provides them (e.g. WorldChatContext, PrivateChatContext)
     * Also restore messages from chatStore if available
     */
    private void loadHistoryIfAvailable() {
        List<ChatMessage> history = null;
        
        if (context instanceof com.example.memorygame.controller.chat.contexts.WorldChatContext) {
            com.example.memorygame.controller.chat.contexts.WorldChatContext wc = 
                (com.example.memorygame.controller.chat.contexts.WorldChatContext) context;
            history = wc.getHistoryMessages();
            System.out.println("[ChatComponent] Loaded " + history.size() + " history messages for world chat");
        } else if (context instanceof com.example.memorygame.controller.chat.contexts.PrivateChatContext) {
            com.example.memorygame.controller.chat.contexts.PrivateChatContext pc = 
                (com.example.memorygame.controller.chat.contexts.PrivateChatContext) context;
            history = pc.getHistoryMessages();
            System.out.println("[ChatComponent] Loaded " + history.size() + " history messages for private chat");
        }
        
        // Merge with messages from chatStore (real-time messages received while tab was closed)
        String channelId = context.getChannelId();
        List<ChatMessage> storedMessages = tcpClient.getRecentMessages(channelId, 1000); // Get up to 1000 stored messages
        
        // Sort stored messages by timestamp first (they may be out of order)
        if (storedMessages != null && !storedMessages.isEmpty()) {
            storedMessages.sort((m1, m2) -> {
                if (m1.getTimestamp() == null && m2.getTimestamp() == null) return 0;
                if (m1.getTimestamp() == null) return -1;
                if (m2.getTimestamp() == null) return 1;
                return m1.getTimestamp().compareTo(m2.getTimestamp());
            });
        }
        
        // Combine history and stored messages, remove duplicates by message ID
        java.util.Map<String, ChatMessage> messageMap = new java.util.HashMap<>();
        
        // Add history messages first (already sorted: oldest first, newest last)
        if (history != null) {
            for (ChatMessage msg : history) {
                if (msg.getId() != null) {
                    messageMap.put(msg.getId(), msg);
                }
            }
        }
        
        // Add stored messages (newer real-time messages, now sorted by timestamp)
        for (ChatMessage msg : storedMessages) {
            if (msg.getId() != null && !messageMap.containsKey(msg.getId())) {
                messageMap.put(msg.getId(), msg);
            }
        }
        
        // Sort by timestamp ascending (oldest first, newest last) - this is correct for chat UI
        List<ChatMessage> allMessages = new java.util.ArrayList<>(messageMap.values());
        allMessages.sort((m1, m2) -> {
            if (m1.getTimestamp() == null && m2.getTimestamp() == null) return 0;
            if (m1.getTimestamp() == null) return -1;
            if (m2.getTimestamp() == null) return 1;
            // Sort ascending: older messages first, newer messages last
            return m1.getTimestamp().compareTo(m2.getTimestamp());
        });
        
        // Debug: Log first and last message timestamps
        if (!allMessages.isEmpty()) {
            ChatMessage first = allMessages.get(0);
            ChatMessage last = allMessages.get(allMessages.size() - 1);
            System.out.println("[ChatComponent] Message order: First=" + first.getTimestamp() + 
                             " (" + (first.getContent() != null && first.getContent().length() > 20 ? 
                                   first.getContent().substring(0, 20) + "..." : first.getContent()) + ")" +
                             ", Last=" + last.getTimestamp() + 
                             " (" + (last.getContent() != null && last.getContent().length() > 20 ? 
                                   last.getContent().substring(0, 20) + "..." : last.getContent()) + ")");
        }
        
        // Add all messages to list
        messagesList.clear();
        messagesList.addAll(allMessages);
        
        // Render all messages
        renderAllMessages();
        
        System.out.println("[ChatComponent] Displayed " + allMessages.size() + " total messages (history + stored)");
        
        // Scroll to bottom after loading all messages
        Platform.runLater(() -> {
            if (scrollPane != null) {
                scrollPane.setVvalue(1.0);
            }
        });
    }
    
    /**
     * Render all messages from messagesList to UI
     */
    private void renderAllMessages() {
        if (messageContainer == null) return;
        
        // Clear current UI
        messageContainer.getChildren().clear();
        
        // Render all messages in order with timestamp logic
        System.out.println("[ChatComponent] Rendering " + messagesList.size() + " messages");
        for (int i = 0; i < messagesList.size(); i++) {
            ChatMessage currentMsg = messagesList.get(i);
            ChatMessage previousMsg = i > 0 ? messagesList.get(i - 1) : null;
            
            System.out.println("[ChatComponent] Message " + i + ": " + 
                             (currentMsg.getTimestamp() != null ? currentMsg.getTimestamp() : "NO_TIMESTAMP") + 
                             ", Previous: " + (previousMsg != null && previousMsg.getTimestamp() != null ? 
                             previousMsg.getTimestamp() : "NO_PREVIOUS"));
            
            // Đảm bảo sender info đồng nhất với otherUser trong private chat
            ChatMessage msgToDisplay = ensureSenderInfoConsistent(currentMsg);
            MessageCell messageCell = new MessageCell(msgToDisplay, context.getCurrentUser(), MessageCell.LayoutMode.DEFAULT, previousMsg, context);
            messageContainer.getChildren().add(messageCell);
        }
    }
    

    private void loadFXML() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/memorygame/chat/base_chat.fxml"));
        loader.setRoot(this);
        loader.setController(this);
        try {
            loader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void setupUI() {
        // Guard for missing controls when FXML is not fully wired
        if (emojiButton != null) {
            emojiButton.setVisible(context.showEmoji());
            emojiButton.setOnAction(e -> toggleStickerPicker());
        }
        // Apply styles based on context.getType()
        this.getStyleClass().add(context.getType().toString().toLowerCase());
        
        // Initialize sticker picker
        initializeStickerPicker();
    }
    
    private void initializeStickerPicker() {
        stickerPicker = new StickerPicker();
        stickerPicker.setOnStickerSelected(sticker -> {
            handleStickerSelected(sticker);
            hideStickerPicker();
        });
        
        stickerPopup = new Popup();
        stickerPopup.getContent().add(stickerPicker);
        stickerPopup.setAutoHide(true);
    }
    
    private void toggleStickerPicker() {
        if (stickerPopup == null || emojiButton == null) return;
        
        if (stickerPopup.isShowing()) {
            hideStickerPicker();
        } else {
            showStickerPicker();
        }
    }
    
    private void showStickerPicker() {
        if (stickerPopup == null || emojiButton == null) return;
        
        // Get button position in scene
        Bounds bounds = emojiButton.localToScene(emojiButton.getBoundsInLocal());
        double x = bounds.getMinX() + emojiButton.getScene().getWindow().getX();
        double y = bounds.getMinY() + emojiButton.getScene().getWindow().getY();
        
        // Position popup above the button (or below if not enough space)
        double popupY = y - stickerPicker.getHeight() - 5;
        if (popupY < emojiButton.getScene().getWindow().getY()) {
            popupY = y + bounds.getHeight() + 5;
        }
        
        stickerPopup.show(emojiButton.getScene().getWindow(), x, popupY);
    }
    
    private void hideStickerPicker() {
        if (stickerPopup != null && stickerPopup.isShowing()) {
            stickerPopup.hide();
        }
    }
    
    private void handleStickerSelected(Sticker sticker) {
        if (!context.canSendMessage()) {
            showError("Cannot send message at this time");
            return;
        }
        
        if (sticker == null || sticker.getId() == null) {
            showError("Invalid sticker");
            return;
        }
        
        SoundManager.playSound("button.wav");
        
        // Create sticker message
        ChatMessage message = new ChatMessage(
            UUID.randomUUID().toString(),
            "", // Empty content for sticker
            context.getCurrentUser(),
            context.getChannelId(),
            context.getType()
        );
        message.setMessageType(MessageType.STICKER);
        message.setStickerId(String.valueOf(sticker.getId()));
        
        // For private chat, set receiver from context
        if (context instanceof com.example.memorygame.controller.chat.contexts.PrivateChatContext) {
            com.example.memorygame.controller.chat.contexts.PrivateChatContext pc = 
                (com.example.memorygame.controller.chat.contexts.PrivateChatContext) context;
            message.setReceiver(pc.getOtherUser());
        }
        
        // Don't set timestamp here - server will set it and return via TCP
        tcpClient.sendChatMessage(message);
        
        // Call callback to notify about sent message (for conversation list update)
        if (onMessageSentCallback != null) {
            onMessageSentCallback.accept(message);
        }
        
        // Don't update UI immediately - wait for server response via TCP
    }

    @FXML
    private void initialize() {
        // This listener robustly scrolls to the bottom whenever the size of the
        // message container changes. This is the definitive fix for all race conditions,
        // both on initial load and for new incoming messages.
        messageContainer.heightProperty().addListener((obs, oldHeight, newHeight) -> {
            if (newHeight.doubleValue() > oldHeight.doubleValue()) {
                 Platform.runLater(() -> scrollPane.setVvalue(1.0));
            }
        });
        
        // Setup input handling
        if (sendButton != null) sendButton.setOnAction(e -> sendMessage());
        if (inputField != null) inputField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                sendMessage();
            }
        });
    }
    
    private void setupMessageHandling() {
        // Subscribe to incoming messages for this channel and render them
        try {
            tcpClient.subscribeToChannel(context.getChannelId(), msg -> {
                Platform.runLater(() -> addMessageToUI(msg));
            });
        } catch (Exception ignored) {
            // Non-fatal for now
        }
    }

    private void addMessageToUI(ChatMessage msg) {
        if (messageContainer == null) return;
        
        // Check if message already exists in list (avoid duplicates)
        for (ChatMessage existing : messagesList) {
            if (existing.getId() != null && existing.getId().equals(msg.getId())) {
                // Message already exists, skip
                return;
            }
        }
        
        // Debug log
        System.out.println("[ChatComponent] Adding message - context.getCurrentUser(): " + 
                         (context.getCurrentUser() != null ? context.getCurrentUser().id : "null") +
                         ", msg.getSender(): " + 
                         (msg.getSender() != null ? msg.getSender().id : "null"));
        
        // Add to list
        messagesList.add(msg);
        
        // Sort list by timestamp (oldest first, newest last) - tin nhắn cũ ở đầu, tin mới ở cuối
        messagesList.sort((m1, m2) -> {
            if (m1.getTimestamp() == null && m2.getTimestamp() == null) return 0;
            if (m1.getTimestamp() == null) return -1;
            if (m2.getTimestamp() == null) return 1;
            return m1.getTimestamp().compareTo(m2.getTimestamp());
        });
        
        // Check if message already in UI (avoid duplicate rendering)
        boolean alreadyInUI = false;
        for (javafx.scene.Node node : messageContainer.getChildren()) {
            if (node instanceof MessageCell) {
                MessageCell cell = (MessageCell) node;
                if (cell.getMessage() != null && cell.getMessage().getId() != null && 
                    cell.getMessage().getId().equals(msg.getId())) {
                    alreadyInUI = true;
                    break;
                }
            }
        }
        
        if (!alreadyInUI) {
            // TỐI ƯU: Chỉ thêm MessageCell mới thay vì re-render toàn bộ để tránh flicker (giống PrivateChat)
            ChatMessage previousMsg = null;
            if (!messageContainer.getChildren().isEmpty()) {
                javafx.scene.Node lastNode = messageContainer.getChildren().get(messageContainer.getChildren().size() - 1);
                if (lastNode instanceof MessageCell) {
                    previousMsg = ((MessageCell) lastNode).getMessage();
                }
            }
            
            // Đảm bảo sender info đồng nhất với otherUser trong private chat
            ChatMessage msgToDisplay = ensureSenderInfoConsistent(msg);
            MessageCell newCell = new MessageCell(msgToDisplay, context.getCurrentUser(), MessageCell.LayoutMode.DEFAULT, previousMsg, context);
            messageContainer.getChildren().add(newCell);
            
            System.out.println("[ChatComponent] Added single new message, total messages: " + messageContainer.getChildren().size());
            
            // Scrolling is now handled by the heightProperty listener
            // scrollToBottom();
        }
    }

    /**
     * Public method to add a message (used by PrivateChatController for real-time updates)
     */
    public void addMessage(ChatMessage msg) {
        if (msg != null && msg.getChannelId() != null && msg.getChannelId().equals(context.getChannelId())) {
            addMessageToUI(msg);
        }
    }
    


    private void sendMessage() {
        if (!context.canSendMessage()) {
            showError("Cannot send message at this time");
            return;
        }
        String content = inputField == null ? "" : inputField.getText().trim();
        if (content.isEmpty()) return;
        
        SoundManager.playSound("message.wav");
        
        ChatMessage message = new ChatMessage(
            UUID.randomUUID().toString(),
            content,
            context.getCurrentUser(),
            context.getChannelId(),
            context.getType()
        );
        
        // For private chat, set receiver from context
        if (context instanceof com.example.memorygame.controller.chat.contexts.PrivateChatContext) {
            com.example.memorygame.controller.chat.contexts.PrivateChatContext pc = 
                (com.example.memorygame.controller.chat.contexts.PrivateChatContext) context;
            message.setReceiver(pc.getOtherUser());
        }
        
        // Don't set timestamp here - server will set it and return via TCP
        tcpClient.sendChatMessage(message);
        
        // Call callback to notify about sent message (for conversation list update)
        if (onMessageSentCallback != null) {
            onMessageSentCallback.accept(message);
        }
        
        // Don't update UI immediately - wait for server response via TCP
        // The message will be added to UI when received back from server with proper timestamp
        
        if (inputField != null) inputField.clear();
    }

    /**
     * Đảm bảo sender info trong message đồng nhất với otherUser trong private chat
     * Nếu sender.id == otherUser.id, dùng thông tin từ otherUser để đảm bảo displayName và avatarUrl đúng
     */
    private ChatMessage ensureSenderInfoConsistent(ChatMessage msg) {
        if (msg == null || msg.getSender() == null) {
            return msg;
        }
        
        // Chỉ áp dụng cho private chat
        if (context instanceof com.example.memorygame.controller.chat.contexts.PrivateChatContext) {
            com.example.memorygame.controller.chat.contexts.PrivateChatContext privateContext = 
                (com.example.memorygame.controller.chat.contexts.PrivateChatContext) context;
            com.example.memorygame.model.user.UserSummary otherUser = privateContext.getOtherUser();
            
            if (otherUser != null && msg.getSender().id > 0 && msg.getSender().id == otherUser.id) {
                // Nếu sender là otherUser, dùng thông tin từ otherUser
                com.example.memorygame.model.user.UserSummary sender = msg.getSender();
                
                // Cập nhật displayName và avatarUrl từ otherUser nếu có
                if (otherUser.displayName != null && !otherUser.displayName.isBlank()) {
                    sender.displayName = otherUser.displayName;
                }
                if (otherUser.avatarUrl != null && !otherUser.avatarUrl.isBlank() && !"null".equals(otherUser.avatarUrl)) {
                    sender.avatarUrl = otherUser.avatarUrl;
                }
                // Giữ username từ sender nếu otherUser không có
                if ((sender.username == null || sender.username.isBlank()) && 
                    otherUser.username != null && !otherUser.username.isBlank()) {
                    sender.username = otherUser.username;
                }
            }
        }
        
        return msg;
    }
    
    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setContentText(message);
        a.showAndWait();
    }
}