package com.example.memorygame.model.user;

public class FriendDTO {
    public Long friendRecordId; // id of Friend record (only for pending requests)
    public Long id;
    public String username;
    public String displayName;
    public String avatarUrl;
    public String status; // ONLINE, BUSY, OFFLINE
    public Integer wins;
    public Integer losses;
}

