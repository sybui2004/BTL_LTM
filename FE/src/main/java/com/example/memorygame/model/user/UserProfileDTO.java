package com.example.memorygame.model.user;

import com.example.memorygame.model.game.MatchHistoryDTO;
import java.util.Date;
import java.util.List;

public class UserProfileDTO {
    public Long id;
    public String displayName;
    public String avatarUrl;
    public Date createdAt;
    public List<MatchHistoryDTO> matchHistory;
}

