package com.ltm.memorygame.dto.game.response;

import lombok.*;
import java.util.Date;

import com.ltm.memorygame.model.enums.MatchStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchResponseDTO {
    private Long id;

    private Long roomId;

    private int player1Score;
    private int player2Score;

    private Long themeId;

    private String boardSize;
    private int timePerMove;

    private Date startTime;
    private Date endTime;

    private MatchStatus status;
}