package com.example.memorygame.controller.chat.contexts;

import java.util.ArrayList;
import java.util.List;

import com.example.memorygame.controller.chat.ChatContext;
import com.example.memorygame.model.chat.ChatType;
import com.example.memorygame.model.user.UserSummary;

/**
 * Context cho Lobby Chat - chat trong phòng chờ
 * 
 * Đặc điểm:
 * - Chat trong phòng chờ (có thể nhiều players)
 * - Temporary (xóa khi room giải tán)
 * - UI tích hợp với lobby screen
 * - Phụ thuộc room state
 * - Hiển thị ready status của players
 */
public class LobbyChatContext implements ChatContext {
    private final String roomId;
    private final UserSummary currentUser;
    private final List<UserSummary> participants;
    private boolean roomActive;
    
    public LobbyChatContext(String roomId, UserSummary currentUser) {
        this.roomId = roomId;
        this.currentUser = currentUser;
        this.participants = new ArrayList<>();
        this.roomActive = true;
    }
    
    @Override
    public String getChannelId() {
        return "lobby_" + roomId;
    }
    
    @Override
    public ChatType getType() {
        return ChatType.LOBBY;
    }
    
    @Override
    public UserSummary getCurrentUser() {
        return currentUser;
    }
    
    @Override
    public List<UserSummary> getParticipants() {
        return new ArrayList<>(participants);
    }
    
    @Override
    public boolean canSendMessage() {
        // Chỉ cho phép chat khi room đang active
        return roomActive && currentUser != null;
    }
    
    @Override
    public String getTitle() {
        return "Lobby Chat - Room " + roomId;
    }
    
    @Override
    public boolean showUserStatus() {
        // Hiển thị ready status và avatar
        return true;
    }
    
    @Override
    public boolean showEmoji() {
        return true;
    }
    
    @Override
    public boolean showTimestamp() {
        return false; // Không cần timestamp trong lobby
    }
    
    // Methods để update participants
    public void addParticipant(UserSummary user) {
        if (!participants.contains(user)) {
            participants.add(user);
        }
    }
    
    public void removeParticipant(UserSummary user) {
        participants.remove(user);
    }
    
    public void setParticipants(List<UserSummary> users) {
        this.participants.clear();
        this.participants.addAll(users);
    }
    
    // Getters/setters cho room state
    public String getRoomId() {
        return roomId;
    }
    
    public void setRoomActive(boolean active) {
        this.roomActive = active;
    }
    
    public boolean isRoomActive() {
        return roomActive;
    }
}
