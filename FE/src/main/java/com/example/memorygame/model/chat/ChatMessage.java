package com.example.memorygame.model.chat;

import java.time.LocalDateTime;

import com.example.memorygame.model.user.UserSummary;

/**
 * Model chung cho tin nhắn, được sử dụng để hiển thị trong UI
 */
public class ChatMessage {
    // Constants cho validation
    public static final int MAX_WORLD_CHAT_LENGTH = 200;
    public static final int MAX_PRIVATE_CHAT_LENGTH = 500;
    public static final int MAX_MATCH_CHAT_LENGTH = 100;
    public static final int MAX_LOBBY_CHAT_LENGTH = 200;

    // Core fields
    private String id;                // ID tin nhắn
    private String content;           // Nội dung tin nhắn
    private UserSummary sender;       // Thông tin người gửi
    private UserSummary receiver;     // Thông tin người nhận (cho private chat)
    private LocalDateTime timestamp;  // Thời gian gửi
    private ChatType type;           // WORLD, PRIVATE, MATCH, LOBBY
    private MessageType messageType;  // TEXT, STICKER, SYSTEM, GAME_EVENT
    private String channelId;        // roomId cho MATCH/LOBBY chat
    private MessageStatus status;    // SENT, DELIVERED, READ
    private String stickerId;        // ID của sticker (nếu messageType là STICKER)
    
    // Additional fields
    private String replyTo;          // ID của message được reply (nullable)
    
    public ChatMessage() {
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.SENT;
        this.messageType = MessageType.TEXT;
    }

    public ChatMessage(String id, String content, UserSummary sender, String channelId, ChatType type) {
        this.id = id;
        this.content = content;
        this.sender = sender;
        this.channelId = channelId;
        this.type = type;
        this.timestamp = LocalDateTime.now();
        this.status = MessageStatus.SENT;
        this.messageType = MessageType.TEXT;
    }

    public ChatMessage(String id, String content, UserSummary sender, UserSummary receiver, 
                      String channelId, ChatType type, MessageType messageType, 
                      String stickerId, LocalDateTime timestamp, MessageStatus status) {
        this.id = id;
        this.content = content;
        this.sender = sender;
        this.receiver = receiver;
        this.channelId = channelId;
        this.type = type;
        this.messageType = messageType == null ? MessageType.TEXT : messageType;
        this.stickerId = stickerId;
        this.timestamp = timestamp == null ? LocalDateTime.now() : timestamp;
        this.status = status == null ? MessageStatus.SENT : status;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserSummary getReceiver() {
        return receiver;
    }

    public void setReceiver(UserSummary receiver) {
        this.receiver = receiver;
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public void setMessageType(MessageType messageType) {
        this.messageType = messageType;
    }

    public String getStickerId() {
        return stickerId;
    }

    public void setStickerId(String stickerId) {
        this.stickerId = stickerId;
    }


    public String getReplyTo() {
        return replyTo;
    }

    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

        // Helper method cho validation
        public int getMaxLength() {
            return switch (type) {
                case WORLD -> MAX_WORLD_CHAT_LENGTH;
                case PRIVATE -> MAX_PRIVATE_CHAT_LENGTH;
                case MATCH -> MAX_MATCH_CHAT_LENGTH;
                case LOBBY -> MAX_LOBBY_CHAT_LENGTH;
            };
        }

        public boolean exceedsMaxLength() {
            return content != null && content.length() > getMaxLength();
        }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public UserSummary getSender() {
        return sender;
    }

    public void setSender(UserSummary sender) {
        this.sender = sender;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public ChatType getType() {
        return type;
    }

    public void setType(ChatType type) {
        this.type = type;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "ChatMessage{" +
                "id='" + id + '\'' +
                ", content='" + content + '\'' +
                ", sender=" + (sender != null ? sender.username : "null") +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", channelId='" + channelId + '\'' +
                ", status=" + status +
                '}';
    }
}