package com.ltm.memorygame.dto.game.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinishedMatchRequest {
    @Min(0)
    private int player1Score;
    @Min(0)
    private int player2Score;
}