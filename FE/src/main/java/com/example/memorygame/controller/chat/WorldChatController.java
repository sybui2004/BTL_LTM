package com.example.memorygame.controller.chat;

import java.util.List;
import java.util.UUID;

import com.example.memorygame.controller.chat.contexts.WorldChatContext;
import com.example.memorygame.model.chat.ChatMessage;
import com.example.memorygame.model.chat.ChatType;
import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.utils.TCPClient;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller cho WorldChat.fxml (layout 2 cột)
 *
 * Yêu cầu:
 * - Bên trái: tin của người gửi, có ô input; tin mới nhất đi LÊN (ở đầu danh sách)
 * - Bên phải: tin của đối thủ, không có input; tin mới nhất đi XUỐNG (append cuối)
 * - Cả hai hiện avatar và tên (handled by MessageCell)
 */
public class WorldChatController {

    // Root and panes
    @FXML private VBox rootBox;
    @FXML private HBox splitContainer;
    @FXML private VBox leftPane;
    @FXML private VBox rightPane;
    @FXML private ScrollPane leftScroll;
    @FXML private ScrollPane rightScroll;
    @FXML private VBox leftMessageContainer;
    @FXML private VBox rightMessageContainer;

    // Input (left only)
    @FXML private TextField inputField;
    @FXML private Button sendButton;
    @FXML private Button emojiButton;
    @FXML private Button toggleButton;

    private WorldChatContext context;
    private TCPClient tcpClient;

    public WorldChatController() {
        this.tcpClient = TCPClient.getInstance();
    }

    @FXML
    private void initialize() {
        // Bind left/right width to ~25% each; middle spacer takes the rest (via HBox grow)
        if (rootBox != null && leftPane != null && rightPane != null) {
            leftPane.prefWidthProperty().bind(rootBox.widthProperty().multiply(0.25));
            rightPane.prefWidthProperty().bind(rootBox.widthProperty().multiply(0.25));
        }

        if (sendButton != null) sendButton.setOnAction(e -> sendMessage());
        if (inputField != null) inputField.setOnAction(e -> sendMessage());
    }

    /**
     * Setup world chat với user hiện tại
     */
    public void setCurrentUser(UserSummary currentUser) {
        if (currentUser == null) return;

        // Create context and load history
        context = new WorldChatContext(currentUser);

        // Register handlers
        tcpClient.registerChatHandlers();

        // Subscribe for new messages and render to correct pane
        try {
            tcpClient.subscribeToChannel(context.getChannelId(), msg -> Platform.runLater(() -> addMessage(msg)));
        } catch (Exception ignored) { }

        // Load history
        List<ChatMessage> history = context.getHistoryMessages();
        if (history != null) {
            for (ChatMessage m : history) {
                addMessage(m);
            }
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

        MessageCell cell = new MessageCell(msg, context != null ? context.getCurrentUser() : null);

        if (isMyMessage(msg)) {
            // Add to LEFT at top (index 0)
            leftMessageContainer.getChildren().add(0, cell);
            if (leftScroll != null) {
                leftScroll.layout();
                leftScroll.setVvalue(0.0); // stick to top
            }
        } else {
            // Add to RIGHT at bottom (append)
            rightMessageContainer.getChildren().add(cell);
            if (rightScroll != null) {
                rightScroll.layout();
                rightScroll.setVvalue(1.0); // stick to bottom
            }
        }
    }

    private void sendMessage() {
        if (context == null || inputField == null) return;
        String content = inputField.getText() != null ? inputField.getText().trim() : "";
        if (content.isEmpty()) return;

        ChatMessage message = new ChatMessage(
            UUID.randomUUID().toString(),
            content,
            context.getCurrentUser(),
            context.getChannelId(),
            ChatType.WORLD
        );

        tcpClient.sendChatMessage(message);
        inputField.clear();
    }

    /** Cleanup khi đóng world chat */
    public void cleanup() {
        if (context != null) {
            tcpClient.unsubscribeFromChannel(context.getChannelId());
        }
        if (leftMessageContainer != null) leftMessageContainer.getChildren().clear();
        if (rightMessageContainer != null) rightMessageContainer.getChildren().clear();
    }

    /** Show/hide world chat window (cho floating mode) */
    public void toggleVisibility() {
        if (splitContainer != null) {
            splitContainer.setVisible(!splitContainer.isVisible());
            splitContainer.setManaged(splitContainer.isVisible());
        }
    }
}
