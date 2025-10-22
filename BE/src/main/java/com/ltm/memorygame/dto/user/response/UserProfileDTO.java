package com.ltm.memorygame.dto.user.response;

import com.ltm.memorygame.dto.game.response.MatchHistoryDTO;
import lombok.*;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileDTO {
    private Long id;
    private String displayName;
    private String avatarUrl;
    private Date createdAt;
    private List<MatchHistoryDTO> matchHistory;
}
