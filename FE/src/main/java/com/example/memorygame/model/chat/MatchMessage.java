package com.example.memorygame.model.chat;

import java.time.Instant;

import com.example.memorygame.model.enums.MessageType;

import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class MatchMessage {
    private final StringProperty roomId; // ID của phòng đấu
    private final LongProperty fromUserId;
    private final StringProperty content;
    private final ObjectProperty<Instant> createdAt;
    private final ObjectProperty<MessageType> messageType;
    private final ObjectProperty<Sticker> sticker;

    public MatchMessage(String roomId, Long fromUserId, String content, Instant createdAt, MessageType messageType, Sticker sticker) {
        this.roomId = new SimpleStringProperty(roomId);
        this.fromUserId = new SimpleLongProperty(fromUserId);
        this.content = new SimpleStringProperty(content);
        this.createdAt = new SimpleObjectProperty<>(createdAt);
        this.messageType = new SimpleObjectProperty<>(messageType);
        this.sticker = new SimpleObjectProperty<>(sticker);
    }

    // --- Getters và Properties ---
    public Long getFromUserId() { return fromUserId.get(); }
    public String getContent() { return content.get(); }
    public MessageType getMessageType() { return messageType.get(); }
    public String getRoomId() { return roomId.get(); }

    public LongProperty fromUserIdProperty() { return fromUserId; }
}