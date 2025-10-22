package com.ltm.memorygame.dto.game.request;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvitePlayerRequest {
    @NotNull
    @Positive
    private Long roomId;
    @NotNull
    @Positive
    private Long senderId;
    @NotNull
    @Positive
    private Long targetId;
}
