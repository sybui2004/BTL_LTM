package com.ltm.memorygame.dto.game.request;

import lombok.*;
import jakarta.validation.constraints.Min;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FinishedMatchRequest {
    @Min(0)
    private int player1Score;
    @Min(0)
    private int player2Score;
}