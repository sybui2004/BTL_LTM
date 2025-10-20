package com.ltm.memorygame.dto.user.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingDTO {

    private int musicVolume;

    private int soundFxVolume;

    private boolean notification;

    private String language;

}

