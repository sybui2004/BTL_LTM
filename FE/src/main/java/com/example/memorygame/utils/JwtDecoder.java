package com.example.memorygame.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Base64;
import java.util.Map;

public class JwtDecoder {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Extracts the user ID from the JWT token payload.
     * @param token JWT token string
     * @return User ID or null if extraction fails
     */
    public static Long extractUserId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            // Decode the payload (second part)
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = MAPPER.readValue(payloadJson, Map.class);
            
            Object userId = payload.get("userId");
            if (userId instanceof Number) {
                return ((Number) userId).longValue();
            } else if (userId != null) {
                return Long.parseLong(String.valueOf(userId));
            }
            return null;
        } catch (Exception e) {
            System.err.println("Failed to extract userId from JWT: " + e.getMessage());
            return null;
        }
    }

    /**
     * Extracts the username from the JWT token payload.
     * @param token JWT token string
     * @return Username or null if extraction fails
     */
    public static String extractUsername(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }
            
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            @SuppressWarnings("unchecked")
            Map<String, Object> payload = MAPPER.readValue(payloadJson, Map.class);
            
            Object sub = payload.get("sub");
            return sub != null ? String.valueOf(sub) : null;
        } catch (Exception e) {
            System.err.println("Failed to extract username from JWT: " + e.getMessage());
            return null;
        }
    }
}

