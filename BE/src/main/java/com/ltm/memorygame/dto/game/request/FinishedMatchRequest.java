package com.ltm.memorygame.dto.game.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FinishedMatchRequest {
    private int player1Score;
    private int player2Score;
}