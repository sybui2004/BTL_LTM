package com.ltm.memorygame.dto.game.response;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchHistoryDTO {
    private Long matchId;

    private String opponentUsername;

    private int userScore;

    private int opponentScore;

    private String result;

    private Date playedAt;
}
