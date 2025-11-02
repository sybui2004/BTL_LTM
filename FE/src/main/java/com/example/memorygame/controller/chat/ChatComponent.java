package com.example.memorygame.controller.chat;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.example.memorygame.model.chat.ChatMessage;
import com.example.memorygame.utils.TCPClient;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;

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
    
    private final ChatContext context;
    private final TCPClient tcpClient;
    
    public ChatComponent(ChatContext context) {
        this.context = context;
        this.tcpClient = TCPClient.getInstance();
        loadFXML();
        setupUI();
        setupMessageHandling();
        loadHistoryIfAvailable();
    }
    
    /**
     * Load history messages if context provides them (e.g. WorldChatContext, PrivateChatContext)
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
        
        if (history != null) {
            for (ChatMessage msg : history) {
                addMessageToUI(msg);
            }
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
        if (emojiButton != null) emojiButton.setVisible(context.showEmoji());
        // Apply styles based on context.getType()
        this.getStyleClass().add(context.getType().toString().toLowerCase());
    }

    @FXML
    private void initialize() {
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
        
        // Debug log
        System.out.println("[ChatComponent] Adding message - context.getCurrentUser(): " + 
                         (context.getCurrentUser() != null ? context.getCurrentUser().id : "null") +
                         ", msg.getSender(): " + 
                         (msg.getSender() != null ? msg.getSender().id : "null"));
        
        // Create custom message cell with avatar and bubble
        MessageCell messageCell = new MessageCell(msg, context.getCurrentUser());
        messageContainer.getChildren().add(messageCell);
        // Auto-scroll the scroll pane (assume it's the first child)
        if (!this.getChildren().isEmpty() && this.getChildren().get(0) instanceof javafx.scene.control.ScrollPane) {
            javafx.scene.control.ScrollPane sp = (javafx.scene.control.ScrollPane) this.getChildren().get(0);
            sp.layout();
            sp.setVvalue(1.0);
        }
    }

    private void sendMessage() {
        if (!context.canSendMessage()) {
            showError("Cannot send message at this time");
            return;
        }
        String content = inputField == null ? "" : inputField.getText().trim();
        if (content.isEmpty()) return;
        
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
        
        tcpClient.sendChatMessage(message);
        if (inputField != null) inputField.clear();
    }

    private void showError(String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setContentText(message);
        a.showAndWait();
    }
}