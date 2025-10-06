package com.ltm.memorygame.dto.user.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequest {
    private String username;
    private String password;
    private String displayName;
    private String email;
    private String avatarUrl;
}
