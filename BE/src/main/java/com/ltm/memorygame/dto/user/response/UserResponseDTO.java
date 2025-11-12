package com.ltm.memorygame.dto.user.response;

import com.ltm.memorygame.model.enums.UserStatus;
import lombok.*;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponseDTO {

    private Long id;

    private String username;

    private String displayName;

    private String email;

    private String avatarUrl;

    private Date createdAt;

    private UserStatus status;

    private int score;

    private UserSettingDTO userSetting;

}
