package com.ltm.memorygame.dto.game.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ltm.memorygame.model.enums.InviteStatus;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomInviteResponseDTO {
    private Long id;
    private Long roomId;
    private Long senderId;
    private String senderName;
    private Long receiverId;
    private String receiverName;
    private InviteStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
