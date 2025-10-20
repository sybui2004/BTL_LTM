package com.ltm.memorygame.dto.friend.response;

import com.ltm.memorygame.model.enums.FriendStatus;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendResponseDTO {
    private Long id;          // id của bản ghi Friend
    private Long senderId;
    private Long receiverId;
    private FriendStatus status; // PENDING | ACCEPTED
}
