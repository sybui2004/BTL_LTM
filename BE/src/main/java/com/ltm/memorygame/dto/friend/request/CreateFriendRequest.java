package com.ltm.memorygame.dto.friend.request;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFriendRequest {
    @NotNull
    @Positive
    private Long toUserId;
}