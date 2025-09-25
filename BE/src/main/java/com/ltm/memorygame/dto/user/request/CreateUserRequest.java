package com.ltm.memorygame.dto.user.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor

public class CreateUserRequest {

    private String username;
    private String password;
    private String displayName;
    private String email;
    private String avatarUrl;
}
