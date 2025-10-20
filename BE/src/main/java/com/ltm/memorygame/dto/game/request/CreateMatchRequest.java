package com.ltm.memorygame.dto.game.request;

import lombok.*;
import jakarta.validation.constraints.*;

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