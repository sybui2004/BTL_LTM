package com.ltm.memorygame.dto.game.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThemeResponseDTO {
    private Long id;
    private String name;
    private String southPath;
    private String assetPath;
}


