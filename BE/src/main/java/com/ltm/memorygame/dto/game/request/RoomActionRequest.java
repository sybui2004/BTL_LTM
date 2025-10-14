package com.ltm.memorygame.dto.game.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomActionRequest {
    private Long roomId;
    private Long playerId;
}
