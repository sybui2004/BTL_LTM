package com.ltm.memorygame.dto.game.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class CreateMatchRequest {
    private Long roomId;

    private Long player1Id;

    private Long player2Id;

    private Long themeId;

    private String boardSize;

    private int timePerMove;
}