package com.ltm.memorygame.dto.user.request;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    private String displayName;
    private String avatarUrl;
}

