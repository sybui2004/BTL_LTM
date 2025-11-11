package com.ltm.memorygame.dto.friend.response;

import com.ltm.memorygame.model.enums.UserStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FriendDTO {
    private Long friendRecordId;
    private Long id;
    private String displayName;
    private String avatarUrl;
    private UserStatus status;
}