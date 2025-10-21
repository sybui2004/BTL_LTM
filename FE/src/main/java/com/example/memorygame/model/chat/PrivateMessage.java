package com.example.memorygame.model.chat;

import java.time.Instant;

import com.example.memorygame.model.enums.MessageType;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class PrivateMessage {
    private final LongProperty id;
    private final LongProperty fromUserId;
    private final LongProperty toUserId;
    private final LongProperty matchId;
    private final StringProperty content; 
    private final ObjectProperty<MessageType> messageType;
    private final ObjectProperty<Sticker> sticker;
    private final ObjectProperty<Instant> createdAt;

    public PrivateMessage(Long id, Long fromUserId, Long toUserId, Long matchId, String content, MessageType messageType, Sticker sticker, Instant createdAt) {
        this.id = new SimpleLongProperty(id);
        this.fromUserId = new SimpleLongProperty(fromUserId);
        this.toUserId = new SimpleLongProperty(toUserId);
        this.matchId = (matchId == null) ? new SimpleLongProperty(0) : new SimpleLongProperty(matchId);
        this.content = new SimpleStringProperty(content);
        this.messageType = new SimpleObjectProperty<>(messageType);
        this.sticker = new SimpleObjectProperty<>(sticker);
        this.createdAt = new SimpleObjectProperty<>(createdAt);
    }

    // Getters and Properties for JavaFX
    public Long getFromUserId() { return fromUserId.get(); }
    public String getContent() { return content.get(); }
    public MessageType getMessageType() { return messageType.get(); }
    public Sticker getSticker() { return sticker.get(); }
    public LongProperty fromUserIdProperty() { return fromUserId; }
}