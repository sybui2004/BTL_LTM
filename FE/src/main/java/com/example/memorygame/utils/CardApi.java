package com.example.memorygame.utils;

import com.example.memorygame.model.game.CardDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * API client for card-related operations
 */
public class CardApi {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Get cards for a specific theme and size with room ID for synchronization
     */
    public static List<CardDTO> getCardsForGame(String themeName, String size, Long roomId) {
        try {
            String encodedTheme = URLEncoder.encode(themeName, StandardCharsets.UTF_8);
            String encodedSize = URLEncoder.encode(size, StandardCharsets.UTF_8);
            String endpoint = "/api/cards/game?theme=" + encodedTheme + "&size=" + encodedSize + "&roomId=" + roomId;
            String json = ApiClient.get(endpoint);
            return MAPPER.readValue(json, new TypeReference<List<CardDTO>>(){});
        } catch (Exception e) {
            System.err.println("[CardApi] Failed to get cards for game: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Get cards for a specific theme and size (legacy method without room ID)
     */
    public static List<CardDTO> getCardsForGame(String themeName, String size) {
        try {
            String encodedTheme = URLEncoder.encode(themeName, StandardCharsets.UTF_8);
            String encodedSize = URLEncoder.encode(size, StandardCharsets.UTF_8);
            String endpoint = "/api/cards/game?theme=" + encodedTheme + "&size=" + encodedSize;
            String json = ApiClient.get(endpoint);
            return MAPPER.readValue(json, new TypeReference<List<CardDTO>>(){});
        } catch (Exception e) {
            System.err.println("[CardApi] Failed to get cards for game: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Get all available cards for a theme
     */
    public static List<CardDTO> getCardsByTheme(String themeName) {
        try {
            String endpoint = "/api/cards/theme/" + themeName;
            String json = ApiClient.get(endpoint);
            return MAPPER.readValue(json, new TypeReference<List<CardDTO>>(){});
        } catch (Exception e) {
            System.err.println("[CardApi] Failed to get cards by theme: " + e.getMessage());
            return Collections.emptyList();
        }
    }
}
