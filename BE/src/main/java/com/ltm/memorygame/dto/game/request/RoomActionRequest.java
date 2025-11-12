package com.ltm.memorygame.dto.game.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomActionRequest {
    @NotNull
    @Positive
    private Long roomId;
    @NotNull
    @Positive
    private Long playerId;
}
