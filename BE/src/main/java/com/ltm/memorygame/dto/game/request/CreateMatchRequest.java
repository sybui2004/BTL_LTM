package com.ltm.memorygame.dto.game.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateMatchRequest {
    @NotNull
    @Positive
    private Long roomId;

    @NotNull
    @Positive
    private Long player1Id;

    @NotNull
    @Positive
    private Long player2Id;

    @NotNull
    @Positive
    private Long themeId;

    @NotBlank
    private String boardSize;

    @Min(5)
    @Max(600)
    private int timePerMove;
}