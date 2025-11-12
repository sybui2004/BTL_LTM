package com.ltm.memorygame.dto.user.request;

import com.ltm.memorygame.model.enums.UserStatus;
import lombok.*;
import jakarta.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetStatusRequest {
    @NotNull
    private UserStatus status; // ONLINE | BUSY | OFFLINE
}