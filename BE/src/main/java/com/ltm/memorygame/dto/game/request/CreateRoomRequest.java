package com.ltm.memorygame.dto.game.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateRoomRequest {
    private Long hostId;
    private Long guestId;
}
