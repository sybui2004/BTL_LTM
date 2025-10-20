package com.ltm.memorygame.dto.game.request;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    @NotNull
    @Positive
    private Long hostId;
    @Positive
    private Long guestId;
}
