package com.ltm.memorygame.dto.user.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class UserRankingDTO {

    private Long id;

    private String username;

    private String avatarUrl;

    private int totalScore;

    private int winCount;

}
