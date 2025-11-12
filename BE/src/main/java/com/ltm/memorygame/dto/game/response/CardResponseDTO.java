package com.ltm.memorygame.dto.game.response;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CardResponseDTO {
    private Long id;
    private String imagePath;
}
