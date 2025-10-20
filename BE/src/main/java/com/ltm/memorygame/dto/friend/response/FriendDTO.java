package com.ltm.memorygame.dto.friend.response;

import com.ltm.memorygame.model.enums.UserStatus;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendDTO {
    private Long id;
    private String displayName;
    private String avatarUrl;
    private UserStatus status;
}