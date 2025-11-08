package com.example.memorygame.model.game;

import java.util.Date;

public class MatchHistoryDTO {
    public Long matchId;
    public String opponentUsername;
    public int userScore;
    public int opponentScore;
    public String result; // WIN, LOSE
    public Date playedAt;
}

