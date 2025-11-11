package com.example.memorygame.model.game;

import java.util.Date;

public class MatchHistoryDTO {
    public Long matchId;
    public String opponentUsername;
    public String opponentDisplayName;
    public int userScore;
    public int opponentScore;
    public String result; // WIN, LOSE, DRAW
    public Integer rankPointsChange; // Positive for win, negative for loss
    public Date playedAt;
}

