package com.ltm.memorygame.dto.game.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThemeResponseDTO {
    private Long id;
    private String name;
    private String southPath;
    private String assetPath;
}


