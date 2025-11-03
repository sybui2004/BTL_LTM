package com.ltm.memorygame.service.game;

import com.ltm.memorygame.dao.game.RoomRepository;
import com.ltm.memorygame.dao.game.MatchRepository;
import com.ltm.memorygame.dao.user.UserRepository;
import com.ltm.memorygame.model.enums.MatchStatus;
import com.ltm.memorygame.dto.game.response.CardResponseDTO;
import com.ltm.memorygame.dto.game.request.FinishedMatchRequest;
import com.ltm.memorygame.model.user.User;
import com.ltm.memorygame.tcp.TCPServer;
import com.ltm.memorygame.tcp.TCPMessage;
import com.ltm.memorygame.tcp.ClientHandler;
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
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final MatchRepository matchRepository;
    private final MatchService matchService;
    
    // Store game sessions by room ID
    private final Map<Long, GameSession> gameSessions = new ConcurrentHashMap<>();
    
    @Transactional(readOnly = true)
    public synchronized List<CardResponseDTO> getCardsForGameSession(Long roomId, String themeName, String size) {
        System.out.println("[GameSessionService] getCardsForGameSession called - Room: " + roomId + ", Theme: " + themeName + ", Size: " + size);
        
        // Check if session already exists with matching theme and size
        GameSession session = gameSessions.get(roomId);
        
        if (session == null || !session.matches(themeName, size)) {
            // Clear old session if settings changed
            if (session != null) {
                System.out.println("[GameSessionService] Clearing old session with different settings for room " + roomId);
                gameSessions.remove(roomId);
            }
            
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
            
            // Check if game is over (all cards matched)
            if (session.matchedCards.size() >= cards.size()) {
                System.out.println("[GameSessionService] All cards matched! Game ending...");
                endGameNormally(roomId);
                return;
            }
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
    
    /**
     * Calculate rank points based on the formula
     * Winner: [(winnerPairs + 2) / (loserPairs + 1)] * 10
     * Loser: ⌈(winnerPairs + 1) / (loserPairs + 1) - 1⌉ * 10
     */
    public int calculateWinnerRankPoints(int winnerPairs, int loserPairs) {
        if (loserPairs < 0) loserPairs = 0;
        if (winnerPairs < 0) winnerPairs = 0;
        
        // [(winnerPairs + 2) / (loserPairs + 1)] * 10, use floor to avoid inflating fractional results
        double points = ((double) (winnerPairs + 2)) / (loserPairs + 1);
        int result = (int) Math.floor(points * 10.0);
        return Math.max(0, result);
    }
    
    public int calculateLoserRankPoints(int winnerPairs, int loserPairs) {
        if (loserPairs < 0) loserPairs = 0;
        if (winnerPairs < 0) winnerPairs = 0;
        
        double result = ((double)(winnerPairs + 1) / (loserPairs + 1)) - 1;
        int points = (int) Math.ceil(result) * 10;
        return Math.max(0, points); // Ensure non-negative
    }
    
    /**
     * End game normally when all cards are matched
     */
    @Transactional
    public void endGameNormally(Long roomId) {
        GameSession session = gameSessions.get(roomId);
        if (session == null) {
            System.err.println("[GameSessionService] No game session found for room " + roomId);
            return;
        }
        
        // Get room to determine player IDs
        var room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));
        if (room.getGuest() == null) {
            System.err.println("[GameSessionService] Room has no guest, cannot end game");
            return;
        }
        
        Long player1Id = room.getHost().getId();
        Long player2Id = room.getGuest().getId();
        String player1Username = room.getHost().getUsername();
        String player2Username = room.getGuest().getUsername();
        
        int player1Pairs = session.player1Score;
        int player2Pairs = session.player2Score;
        
        // Determine winner
        boolean player1Wins = player1Pairs > player2Pairs;
        boolean player2Wins = player2Pairs > player1Pairs;
        
        Long winnerId = null;
        Long loserId = null;
        int winnerRankPoints = 0;
        int loserRankPoints = 0;
        
        if (player1Wins) {
            winnerId = player1Id;
            loserId = player2Id;
            winnerRankPoints = calculateWinnerRankPoints(player1Pairs, player2Pairs);
            loserRankPoints = calculateLoserRankPoints(player1Pairs, player2Pairs);
        } else if (player2Wins) {
            winnerId = player2Id;
            loserId = player1Id;
            winnerRankPoints = calculateWinnerRankPoints(player2Pairs, player1Pairs);
            loserRankPoints = calculateLoserRankPoints(player2Pairs, player1Pairs);
        } else {
            // Tie - both lose
            winnerId = null;
        }
        
        // Update user scores in database
        if (winnerId != null) {
            updateUserScore(winnerId, winnerRankPoints, true);
            updateUserScore(loserId, loserRankPoints, false);
        }
        
        // Send game end message to both players
        TCPServer tcpServer = applicationContext.getBean(TCPServer.class);
        Map<String, Object> endData = new HashMap<>();
        endData.put("player1Score", player1Pairs);
        endData.put("player2Score", player2Pairs);
        endData.put("winnerId", winnerId);
        String winnerUsername = winnerId == player1Id ? player1Username : player2Username;
        endData.put("winnerUsername", winnerUsername);
        endData.put("loserId", loserId);
        endData.put("isSurrender", false);
        endData.put("winnerRankPoints", winnerRankPoints);
        endData.put("loserRankPoints", loserRankPoints);
        
        // Get match ID from room
        try {
            matchRepository.findByRoomAndStatus(room, MatchStatus.PLAYING)
                    .ifPresent(match -> endData.put("matchId", match.getId()));
        } catch (Exception e) {
            System.err.println("[GameSessionService] Failed to get matchId: " + e.getMessage());
        }
        
        // After normal end, set room back to READY so players can rematch
        try {
            room.setStatus(com.ltm.memorygame.model.enums.RoomStatus.READY);
            roomRepository.save(room);
        } catch (Exception ignored) {}

        // Send to both players
        TCPMessage endMessage = new TCPMessage("GAME_END", endData, "server", null);
        ClientHandler player1Handler = tcpServer.getClientHandler(player1Username);
        ClientHandler player2Handler = tcpServer.getClientHandler(player2Username);
        
        if (player1Handler != null) {
            try {
                player1Handler.sendMessage(endMessage);
                System.out.println("[GameSessionService] Sent GAME_END to player1: " + player1Username);
            } catch (Exception e) {
                System.err.println("[GameSessionService] Failed to send GAME_END to " + player1Username + ": " + e.getMessage());
            }
        }
        
        if (player2Handler != null) {
            try {
                player2Handler.sendMessage(endMessage);
                System.out.println("[GameSessionService] Sent GAME_END to player2: " + player2Username);
            } catch (Exception e) {
                System.err.println("[GameSessionService] Failed to send GAME_END to " + player2Username + ": " + e.getMessage());
            }
        }
        
        System.out.println("[GameSessionService] Game ended normally - Winner: " + winnerUsername + 
                          " (" + player1Pairs + " vs " + player2Pairs + ")");
        
        // Clear session
        clearGameSession(roomId);
    }
    
    /**
     * Handle player surrender/exit during game
     */
    @Transactional
    public void handlePlayerExit(Long roomId, Long exitedPlayerId, String exitedUsername) {
        GameSession session = gameSessions.get(roomId);
        if (session == null) {
            System.err.println("[GameSessionService] No game session found for room " + roomId);
            return;
        }
        
        // Get room to determine remaining player
        var room = roomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("Room not found: " + roomId));
        
        Long remainingPlayerId = null;
        String remainingUsername = null;
        
        if (room.getHost().getId().equals(exitedPlayerId)) {
            // Host exited, remaining is guest
            if (room.getGuest() != null) {
                remainingPlayerId = room.getGuest().getId();
                remainingUsername = room.getGuest().getUsername();
            }
        } else if (room.getGuest() != null && room.getGuest().getId().equals(exitedPlayerId)) {
            // Guest exited, remaining is host
            remainingPlayerId = room.getHost().getId();
            remainingUsername = room.getHost().getUsername();
        }
        
        if (remainingPlayerId == null) {
            System.err.println("[GameSessionService] Cannot determine remaining player");
            return;
        }
        
        // Determine scores
        // Host is always player1, guest is player2
        boolean exitedIsHost = room.getHost().getId().equals(exitedPlayerId);
        int remainingPairs = exitedIsHost ? session.player2Score : session.player1Score;
        int exitedPairs = exitedIsHost ? session.player1Score : session.player2Score;
        
        // Remaining player wins, exited player loses
        int winnerRankPoints = calculateWinnerRankPoints(remainingPairs, exitedPairs);
        int loserRankPoints = 100; // Fixed 100 points deduction for surrender
        
        // Update user scores
        updateUserScore(remainingPlayerId, winnerRankPoints, true);
        updateUserScore(exitedPlayerId, loserRankPoints, false);
        
        // Send game end message directly to remaining player
        TCPServer tcpServer = applicationContext.getBean(TCPServer.class);
        Map<String, Object> endData = new HashMap<>();
        endData.put("player1Score", exitedIsHost ? exitedPairs : remainingPairs);
        endData.put("player2Score", exitedIsHost ? remainingPairs : exitedPairs);
        endData.put("winnerId", remainingPlayerId);
        endData.put("winnerUsername", remainingUsername);
        endData.put("loserId", exitedPlayerId);
        endData.put("isSurrender", true);
        endData.put("winnerRankPoints", winnerRankPoints);
        endData.put("loserRankPoints", loserRankPoints);
        
        // Get match ID and finish the match
        Long matchId = null;
        try {
            var matchOpt = matchRepository.findByRoomAndStatus(room, MatchStatus.PLAYING);
            if (matchOpt.isPresent()) {
                var match = matchOpt.get();
                matchId = match.getId();
                
                // Finish the match with current scores
                FinishedMatchRequest finishRequest = new FinishedMatchRequest();
                finishRequest.setPlayer1Score(exitedIsHost ? exitedPairs : remainingPairs);
                finishRequest.setPlayer2Score(exitedIsHost ? remainingPairs : exitedPairs);
                
                try {
                    matchService.finishMatch(match.getId(), finishRequest);
                    System.out.println("[GameSessionService] ✓ Finished match " + match.getId() + " due to player exit - Scores: P1=" + 
                                     finishRequest.getPlayer1Score() + ", P2=" + finishRequest.getPlayer2Score());
                } catch (Exception e) {
                    System.err.println("[GameSessionService] ✗ Failed to finish match " + match.getId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                // Match not found - this can happen if player disconnects before host starts the game
                System.out.println("[GameSessionService] No match found for room " + roomId + " (status: " + room.getStatus() + 
                                 "). This may happen if player disconnected before game started. Skipping match finish.");
            }
        } catch (Exception e) {
            System.err.println("[GameSessionService] Exception while getting matchId for room " + roomId + ": " + e.getMessage());
            e.printStackTrace();
        }
        
        // Add matchId to GAME_END message
        if (matchId != null) {
            endData.put("matchId", matchId);
        }
        
        System.out.println("[GameSessionService] Preparing GAME_END message for remaining player: " + remainingUsername);
        System.out.println("[GameSessionService] End data: " + endData);
        
        TCPMessage endMessage = new TCPMessage("GAME_END", endData, "server", remainingUsername);
        // Send directly to remaining player instead of broadcasting to all
        ClientHandler remainingHandler = tcpServer.getClientHandler(remainingUsername);
        
        System.out.println("[GameSessionService] Looking for handler for: " + remainingUsername);
        System.out.println("[GameSessionService] Handler found: " + (remainingHandler != null));
        
        boolean delivered = false;
        if (remainingHandler != null) {
            try {
                remainingHandler.sendMessage(endMessage);
                delivered = true;
                System.out.println("[GameSessionService] ✓ Successfully sent GAME_END to remaining player: " + remainingUsername);
            } catch (Exception e) {
                System.err.println("[GameSessionService] ✗ Failed to send GAME_END to " + remainingUsername + ": " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("[GameSessionService] ✗ Remaining player " + remainingUsername + " not found in online clients");
            System.err.println("[GameSessionService] This means the player is not connected or has already disconnected");
        }

        // Fallback: if direct delivery failed, broadcast (no room filtering yet)
        if (!delivered) {
            System.out.println("[GameSessionService] Fallback broadcast GAME_END for room " + roomId);
            tcpServer.broadcastToRoom(roomId, endMessage);
        }
        
        System.out.println("[GameSessionService] Player exited: " + exitedUsername + 
                          " - Winner: " + remainingUsername);
        
        // Clear session
        clearGameSession(roomId);
    }
    
    /**
     * Update user rank score in database
     */
    private void updateUserScore(Long userId, int pointsChange, boolean isWinner) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
            
            int oldScore = user.getScore();
            int delta = isWinner ? pointsChange : -pointsChange;
            int newScore = user.getScore() + delta;
            if (newScore < 0) {
                System.out.println("[GameSessionService] Score would go negative, clamping to 0: " + newScore + " -> 0");
                newScore = 0; // Score cannot go below 0
            }
            
            System.out.println("[GameSessionService] Updating user " + userId + " score: " + oldScore + 
                              (isWinner ? " +" : " -") + Math.abs(pointsChange) + " = " + newScore);
            
            user.setScore(newScore);
            try {
                // Persist immediately to avoid delays from outer transactional boundaries
                userRepository.saveAndFlush(user);
            } catch (NoSuchMethodError | NoClassDefFoundError e) {
                // Fallback for repositories that don't support saveAndFlush
                userRepository.save(user);
                try { userRepository.flush(); } catch (Exception ignore) {}
            }
            
            // Verify the save
            User verify = userRepository.findById(userId).orElse(null);
            if (verify != null) {
                System.out.println("[GameSessionService] ✓ Verified user " + userId + " score saved: " + verify.getScore());
            } else {
                System.err.println("[GameSessionService] ✗ Failed to verify save for user " + userId);
            }
        } catch (Exception e) {
            System.err.println("[GameSessionService] ✗ Failed to update user score: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // Inner class to store game session data
    private static class GameSession {
        private final String themeName;
        private final String size;
        private final List<CardResponseDTO> cards;
        private int player1Score = 0;
        private int player2Score = 0;
        private Set<Integer> matchedCards = new HashSet<>();
        
        public GameSession(Long roomId, String themeName, String size, List<CardResponseDTO> cards) {
            this.themeName = themeName;
            this.size = size;
            this.cards = cards;
        }
        
        public boolean matches(String themeName, String size) {
            return this.themeName.equals(themeName) && this.size.equals(size);
        }
        
        public List<CardResponseDTO> getCards() { 
            return cards; 
        }
    }
}
