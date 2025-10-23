package com.ltm.memorygame.service.game;

import com.ltm.memorygame.dto.game.response.CardResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GameSessionService {
    
    private final CardService cardService;
    
    // Store game sessions by room ID
    private final Map<Long, GameSession> gameSessions = new ConcurrentHashMap<>();
    
    @Transactional(readOnly = true)
    public List<CardResponseDTO> getCardsForGameSession(Long roomId, String themeName, String size) {
        // Check if session already exists
        GameSession session = gameSessions.get(roomId);
        
        if (session == null) {
            // Create new session
            List<CardResponseDTO> cards = cardService.getCardsForGame(themeName, size);
            session = new GameSession(roomId, themeName, size, cards);
            gameSessions.put(roomId, session);
            System.out.println("[GameSessionService] Created new game session for room " + roomId + " with " + cards.size() + " cards");
        } else {
            System.out.println("[GameSessionService] Using existing game session for room " + roomId + " with " + session.getCards().size() + " cards");
        }
        
        return session.getCards();
    }
    
    public void clearGameSession(Long roomId) {
        gameSessions.remove(roomId);
        System.out.println("[GameSessionService] Cleared game session for room " + roomId);
    }
    
    // Inner class to store game session data
    private static class GameSession {
        private final List<CardResponseDTO> cards;
        
        public GameSession(Long roomId, String themeName, String size, List<CardResponseDTO> cards) {
            this.cards = cards;
        }
        
        public List<CardResponseDTO> getCards() { return cards; }
    }
}
