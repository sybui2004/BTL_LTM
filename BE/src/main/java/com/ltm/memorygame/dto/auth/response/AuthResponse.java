package com.ltm.memorygame.dto.auth.response;

import com.ltm.memorygame.dto.user.response.UserResponseDTO;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {
    private String token;
    private UserResponseDTO user;
}