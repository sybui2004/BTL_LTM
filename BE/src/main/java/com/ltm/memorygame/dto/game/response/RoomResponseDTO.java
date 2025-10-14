package com.ltm.memorygame.dto.game.response;

import com.ltm.memorygame.model.enums.RoomStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponseDTO {
    private Long id;
    private Long hostId;
    private Long guestId;
    private RoomStatus status;
}
