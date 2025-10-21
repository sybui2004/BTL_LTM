package com.example.memorygame.model.chat;

import java.time.Instant;

import com.example.memorygame.model.enums.MessageType;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class WorldMessage {
    private final LongProperty id;
    private final LongProperty senderId;
    private final StringProperty senderName;
    private final StringProperty content;
    private final StringProperty avatarUrl;
    private final ObjectProperty<MessageType> messageType;
    private final ObjectProperty<Sticker> sticker;
    private final ObjectProperty<Instant> createdAt;

    public WorldMessage(Long id, Long senderId, String senderName, String content, String avatarUrl, MessageType messageType, Sticker sticker, Instant createdAt) {
        this.id = new SimpleLongProperty(id);
        this.senderId = new SimpleLongProperty(senderId);
        this.senderName = new SimpleStringProperty(senderName);
        this.content = new SimpleStringProperty(content);
        this.avatarUrl = new SimpleStringProperty(avatarUrl);
        this.messageType = new SimpleObjectProperty<>(messageType);
        this.sticker = new SimpleObjectProperty<>(sticker);
        this.createdAt = new SimpleObjectProperty<>(createdAt);
    }

    // --- Getters và Properties (ví dụ) ---
    public Long getSenderId() { return senderId.get(); }
    public String getContent() { return content.get(); }
    public MessageType getMessageType() { return messageType.get(); }
    public String getSenderName() { return senderName.get(); }

    public LongProperty senderIdProperty() { return senderId; }
    public StringProperty contentProperty() { return content; }
}