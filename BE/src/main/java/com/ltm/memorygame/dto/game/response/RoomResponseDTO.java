package com.ltm.memorygame.dto.game.response;

import com.ltm.memorygame.model.enums.RoomStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomResponseDTO {
    private Long id;
    private Long hostId;
    private Long guestId;
    private RoomStatus status;
}
