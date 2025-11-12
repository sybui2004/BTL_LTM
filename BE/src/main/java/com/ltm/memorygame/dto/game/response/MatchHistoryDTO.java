package com.ltm.memorygame.dto.game.response;
import lombok.*;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchHistoryDTO {
    private Long matchId;

    private String opponentUsername;
    
    private String opponentDisplayName;

    private int userScore;

    private int opponentScore;

    private String result;
    
    private Integer rankPointsChange; // Positive for win, negative for loss

    private Date playedAt;
}
