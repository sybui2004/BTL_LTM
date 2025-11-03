package com.example.memorygame.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.util.HashMap;
import java.util.Map;

public class MatchApi {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Create a match (host only) and return the new match ID
     */
    public static Long createMatch(Long roomId,
                                   Long player1Id,
                                   Long player2Id,
                                   Long themeId,
                                   String boardSize,
                                   int timePerMove) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("player1Id", player1Id);
            request.put("player2Id", player2Id);
            request.put("themeId", themeId);
            request.put("boardSize", boardSize);
            request.put("timePerMove", timePerMove);

            String json = MAPPER.writeValueAsString(request);
            String response = ApiClient.postJsonAuth("/api/rooms/start", json);

            // Response is MatchResponseDTO; we only need the id
            Map<?, ?> resp = MAPPER.readValue(response, Map.class);
            Object idObj = resp.get("id");
            if (idObj instanceof Number) {
                return ((Number) idObj).longValue();
            }
            if (idObj != null) {
                try { return Long.parseLong(idObj.toString()); } catch (NumberFormatException ignored) {}
            }
            return null;
        } catch (Exception e) {
            System.err.println("[MatchApi] Failed to create match: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Finish a match with final scores
     * @param matchId The match ID
     * @param player1Score Player 1's final score
     * @param player2Score Player 2's final score
     * @return true if successful, false otherwise
     */
    public static boolean finishMatch(Long matchId, int player1Score, int player2Score) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("player1Score", player1Score);
            request.put("player2Score", player2Score);
            
            String json = MAPPER.writeValueAsString(request);
            ApiClient.putJsonAuth("/api/matches/" + matchId + "/finish", json);
            System.out.println("[MatchApi] Successfully finished match " + matchId + " with scores P1: " + player1Score + ", P2: " + player2Score);
            return true;
        } catch (Exception e) {
            System.err.println("[MatchApi] Failed to finish match " + matchId + ": " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}

