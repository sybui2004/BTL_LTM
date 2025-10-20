package com.ltm.memorygame.dto.game.request;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomExitRequest {
    @NotNull
    @Positive
    private Long roomId;
    @NotNull
    @Positive
    private Long playerId;
}
