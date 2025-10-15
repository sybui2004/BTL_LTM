package com.ltm.memorygame.dto.user.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSettingDTO {

    private int musicVolume;

    private int soundFxVolume;

    private boolean notification;

    private String language;

}

