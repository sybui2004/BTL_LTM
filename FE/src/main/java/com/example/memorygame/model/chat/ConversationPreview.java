package com.example.memorygame.model.chat;

import java.time.Instant;

import com.example.memorygame.model.enums.MessageType;
import com.example.memorygame.model.user.UserSummary;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ConversationPreview {
    private final SimpleObjectProperty<UserSummary> otherUser;
    private final StringProperty lastMessageContent;
    private final SimpleObjectProperty<MessageType> messageType;
    private final ObjectProperty<Instant> createdAt;

    public ConversationPreview(UserSummary otherUser, String lastMessageContent, MessageType messageType, Instant createdAt) {
        this.otherUser = new SimpleObjectProperty<>(otherUser);
        this.lastMessageContent = new SimpleStringProperty(lastMessageContent);
        this.messageType = new SimpleObjectProperty<>(messageType);
        this.createdAt = new SimpleObjectProperty<>(createdAt);
    }

    // --- Getters and Property  ---
    
    public UserSummary getOtherUser() {
        return otherUser.get();
    }

    public ObjectProperty<UserSummary> otherUserProperty() {
        return otherUser;
    }

    public String getLastMessageContent() {
        return lastMessageContent.get();
    }

    public StringProperty lastMessageContentProperty() {
        return lastMessageContent;
    }

    public MessageType getMessageType() {
        return messageType.get();
    }

    public ObjectProperty<MessageType> messageTypeProperty() {
        return messageType;
    }

    public Instant getCreatedAt() {
        return createdAt.get();
    }

    public ObjectProperty<Instant> createdAtProperty() {
        return createdAt;
    }
}