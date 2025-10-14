package com.ltm.memorygame.dto.game.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomExitRequest {
    private Long roomId;
    private Long playerId;
}
