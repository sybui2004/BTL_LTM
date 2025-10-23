package com.ltm.memorygame.service.game;

import com.ltm.memorygame.dao.game.CardRepository;
import com.ltm.memorygame.dto.game.response.CardResponseDTO;
import com.ltm.memorygame.mapper.CardMapper;
import com.ltm.memorygame.model.game.Card;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardService {
    
    private final CardRepository cardRepository;
    
    @Transactional(readOnly = true)
    public List<CardResponseDTO> getCardsForGame(String themeName, String size) {
        // Get all cards for theme
        List<Card> allCards = cardRepository.findByThemeName(themeName);
        
        // Calculate how many unique cards we need
        int totalCards = getTotalCards(size);
        int uniqueCards = totalCards / 2; // Each card appears twice
        
        // Take only the first N unique cards
        List<Card> selectedCards = allCards.stream()
                .distinct() // Remove duplicates by cardPath
                .limit(uniqueCards)
                .collect(Collectors.toList());
        
        // Duplicate each card to create pairs
        List<Card> gameCards = new ArrayList<>();
        for (Card card : selectedCards) {
            gameCards.add(card); // First copy
            gameCards.add(card); // Second copy (duplicate)
        }
        
        // Shuffle the cards
        Collections.shuffle(gameCards);
        
        // Convert to DTOs
        return gameCards.stream()
                .map(CardMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    private int getTotalCards(String size) {
        switch (size) {
            case "5x6":
                return 30; // 5 rows × 6 columns
            case "6x7":
                return 42; // 6 rows × 7 columns
            default:
                return 30; // Default to 5x6
        }
    }
    
    @Transactional(readOnly = true)
    public List<CardResponseDTO> getAllCards() {
        return cardRepository.findAll().stream()
                .map(CardMapper::toDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public CardResponseDTO getCardById(Long id) {
        return cardRepository.findById(id)
                .map(CardMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Card not found with id: " + id));
    }
}
