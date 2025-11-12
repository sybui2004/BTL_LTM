package com.ltm.memorygame.dto.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 32)
    private String username;

    @NotBlank
    @Size(min = 6, max = 128)
    private String password;

    @NotBlank
    @Email
    private String email;
}


