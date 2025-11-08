package com.ltm.memorygame.mapper;

import com.ltm.memorygame.dto.game.response.CardResponseDTO;
import com.ltm.memorygame.model.game.Card;

public class CardMapper {
    
    public static CardResponseDTO toDTO(Card card) {
        if (card == null) {
            return null;
        }
        
        return CardResponseDTO.builder()
                .id(card.getId())
                .imagePath(card.getCardPath())
                .build();
    }
}
