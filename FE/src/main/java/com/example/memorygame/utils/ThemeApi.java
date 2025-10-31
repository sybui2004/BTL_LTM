package com.example.memorygame.utils;

import com.example.memorygame.model.game.ThemeDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

public class ThemeApi {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static List<ThemeDTO> getAllThemes() {
        try {
            String json = ApiClient.get("/api/themes");
            return MAPPER.readValue(json, new TypeReference<List<ThemeDTO>>(){});
        } catch (Exception e) {
            System.err.println("[ThemeApi] Failed to get themes: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static ThemeDTO getThemeById(Long id) {
        try {
            String json = ApiClient.get("/api/themes/" + id);
            return MAPPER.readValue(json, ThemeDTO.class);
        } catch (Exception e) {
            System.err.println("[ThemeApi] Failed to get theme by id: " + e.getMessage());
            return null;
        }
    }
}
