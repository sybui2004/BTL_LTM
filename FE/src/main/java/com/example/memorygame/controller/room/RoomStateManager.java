package com.example.memorygame.controller.room;

/**
 * Manages room state (current room ID, guest/host IDs, host status)
 */
public class RoomStateManager {
    private Long currentRoomId = null;
    private Long currentGuestId = null;
    private Long currentHostId = null;
    private boolean isHost = true;
    
    public Long getCurrentRoomId() {
        return currentRoomId;
    }
    
    public void setCurrentRoomId(Long currentRoomId) {
        this.currentRoomId = currentRoomId;
    }
    
    public Long getCurrentGuestId() {
        return currentGuestId;
    }
    
    public void setCurrentGuestId(Long currentGuestId) {
        this.currentGuestId = currentGuestId;
    }
    
    public Long getCurrentHostId() {
        return currentHostId;
    }
    
    public void setCurrentHostId(Long currentHostId) {
        this.currentHostId = currentHostId;
    }
    
    public boolean isHost() {
        return isHost;
    }
    
    public void setHost(boolean isHost) {
        this.isHost = isHost;
    }
    
    public boolean isRoomFull() {
        return currentGuestId != null || currentHostId != null;
    }
}

