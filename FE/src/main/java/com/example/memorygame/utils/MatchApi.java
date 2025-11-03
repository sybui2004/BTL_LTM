package com.example.memorygame.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.util.HashMap;
import java.util.Map;

public class MatchApi {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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

