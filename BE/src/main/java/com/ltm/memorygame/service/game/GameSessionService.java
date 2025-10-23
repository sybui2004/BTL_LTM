package com.ltm.memorygame.service.game;

import com.ltm.memorygame.dto.game.response.CardResponseDTO;
import com.ltm.memorygame.tcp.TCPServer;
import com.ltm.memorygame.tcp.TCPMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class GameSessionService {
    
    private final CardService cardService;
    private final ApplicationContext applicationContext;
    
    // Store game sessions by room ID
    private final Map<Long, GameSession> gameSessions = new ConcurrentHashMap<>();
    
    @Transactional(readOnly = true)
    public synchronized List<CardResponseDTO> getCardsForGameSession(Long roomId, String themeName, String size) {
        System.out.println("[GameSessionService] getCardsForGameSession called - Room: " + roomId + ", Theme: " + themeName + ", Size: " + size);
        
        // Check if session already exists (double-check pattern)
        GameSession session = gameSessions.get(roomId);
        
        if (session == null) {
            // Create new session
            System.out.println("[GameSessionService] Creating NEW session for room " + roomId);
            List<CardResponseDTO> cards = cardService.getCardsForGame(themeName, size, roomId);
            session = new GameSession(roomId, themeName, size, cards);
            gameSessions.put(roomId, session);
            System.out.println("[GameSessionService] Created new game session for room " + roomId + " with " + cards.size() + " cards");
            
            // Log first few cards for debugging
            for (int i = 0; i < Math.min(5, cards.size()); i++) {
                System.out.println("[GameSessionService] Card " + i + ": " + cards.get(i).getImagePath());
            }
        } else {
            System.out.println("[GameSessionService] Using EXISTING session for room " + roomId + " with " + session.getCards().size() + " cards");
            
            // Log first few cards for debugging
            List<CardResponseDTO> cards = session.getCards();
            for (int i = 0; i < Math.min(5, cards.size()); i++) {
                System.out.println("[GameSessionService] Existing Card " + i + ": " + cards.get(i).getImagePath());
            }
        }
        
        return session.getCards();
    }
    
    public void clearGameSession(Long roomId) {
        gameSessions.remove(roomId);
        System.out.println("[GameSessionService] Cleared game session for room " + roomId);
    }
    
    /**
     * Process cards for match check
     */
    public void processCardsForMatch(Long roomId, Integer cardIndex1, Integer cardIndex2, Boolean isHost, String username) {
        GameSession session = gameSessions.get(roomId);
        if (session == null) {
            System.err.println("[GameSessionService] No game session found for room " + roomId);
            return;
        }
        
        System.out.println("[GameSessionService] Processing match check for room " + roomId + 
                          " - Cards: " + cardIndex1 + ", " + cardIndex2 + " by " + username);
        
        // Get cards from session
        List<CardResponseDTO> cards = session.getCards();
        if (cardIndex1 >= cards.size() || cardIndex2 >= cards.size()) {
            System.err.println("[GameSessionService] Invalid card indices: " + cardIndex1 + ", " + cardIndex2);
            return;
        }
        
        // Compare cards
        CardResponseDTO card1 = cards.get(cardIndex1);
        CardResponseDTO card2 = cards.get(cardIndex2);
        
        System.out.println("[GameSessionService] Comparing cards:");
        System.out.println("[GameSessionService] Card " + cardIndex1 + " (index): " + card1.getImagePath());
        System.out.println("[GameSessionService] Card " + cardIndex2 + " (index): " + card2.getImagePath());
        
        boolean isMatch = card1.getImagePath().equals(card2.getImagePath());
        
        System.out.println("[GameSessionService] Match result: " + isMatch + 
                          " - Card1: " + card1.getImagePath() + 
                          " - Card2: " + card2.getImagePath());
        
        // Update game state
        if (isMatch) {
            // Cards match - add score
            if (isHost) {
                session.player1Score++;
            } else {
                session.player2Score++;
            }
            
            // Mark cards as matched
            session.matchedCards.add(cardIndex1);
            session.matchedCards.add(cardIndex2);
            
            System.out.println("[GameSessionService] Cards matched! Scores - P1: " + session.player1Score + ", P2: " + session.player2Score);
        } else {
            System.out.println("[GameSessionService] Cards don't match - will flip back and switch turn");
        }
        
        // Send match result to all players in room
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("isMatch", isMatch);
        resultData.put("cardIndex1", cardIndex1);
        resultData.put("cardIndex2", cardIndex2);
        resultData.put("shouldSwitchTurn", !isMatch); // Switch turn only if no match
        resultData.put("player1Score", session.player1Score);
        resultData.put("player2Score", session.player2Score);
        
        TCPMessage resultMessage = new TCPMessage("MATCH_RESULT", resultData, "server", null);
        
        // Get TCPServer from ApplicationContext to avoid circular dependency
        TCPServer tcpServer = applicationContext.getBean(TCPServer.class);
        tcpServer.broadcastToRoom(roomId, resultMessage);
        
        System.out.println("[GameSessionService] Sent MATCH_RESULT to room " + roomId + ": " + resultData);
    }
    
    // Inner class to store game session data
    private static class GameSession {
        private final List<CardResponseDTO> cards;
        private int player1Score = 0;
        private int player2Score = 0;
        private Set<Integer> matchedCards = new HashSet<>();
        
        public GameSession(Long roomId, String themeName, String size, List<CardResponseDTO> cards) {
            this.cards = cards;
        }
        
        public List<CardResponseDTO> getCards() { 
            return cards; 
        }
    }
}
