package com.example.memorygame.model.user;

public class UserSummary {
    public long id;
    public String username;
    public String displayName; // for /api/users/{id}
    public String status;      // enum string from backend
    public String avatarUrl;
    public int totalScore;
    public int winCount;
}


