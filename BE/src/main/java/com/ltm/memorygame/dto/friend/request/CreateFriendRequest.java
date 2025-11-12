package com.ltm.memorygame.dto.friend.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFriendRequest {
    @NotNull
    @Positive
    private Long toUserId;
}