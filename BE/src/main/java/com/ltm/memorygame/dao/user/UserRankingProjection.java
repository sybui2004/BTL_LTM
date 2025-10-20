package com.ltm.memorygame.dao.user;

public interface UserRankingProjection {
    Long getId();
    String getDisplayName();
    String getAvatarUrl();
    Integer getTotalScore();
    Integer getWinCount();
}