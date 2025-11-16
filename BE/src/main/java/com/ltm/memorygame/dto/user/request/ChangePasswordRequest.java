package com.ltm.memorygame.dto.user.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "Password cannot be blank")
    private String password;
}

