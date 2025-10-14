package com.ltm.memorygame.dto.game.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvitePlayerRequest {
    private Long roomId;
    private Long senderId;
    private Long targetId;
}
