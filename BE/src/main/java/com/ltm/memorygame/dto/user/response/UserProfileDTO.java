package com.ltm.memorygame.dto.user.response;

import com.ltm.memorygame.dto.game.response.MatchHistoryDTO;
import lombok.*;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private Long id;
    private String username;
    private String displayName;
    private String email;
    private String avatarUrl;
    private Date createdAt;
    private UserSettingDTO userSetting;
    private List<MatchHistoryDTO> matchHistory;
}
