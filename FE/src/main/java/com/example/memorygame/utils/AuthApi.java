package com.example.memorygame.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AuthApi {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static String login(String username, String password) {
        try {
            String body = String.format("{\"username\":\"%s\",\"password\":\"%s\"}", username, password);
            String json = ApiClient.postJson("/api/auth/login", body);
            JsonNode root = MAPPER.readTree(json);
            JsonNode tokenNode = root.path("token");
            if (tokenNode.isMissingNode() || tokenNode.asText().isBlank()) {
                throw new IllegalStateException("No token in response");
            }
            return tokenNode.asText();
        } catch (Exception e) {
            throw new RuntimeException("Login failed", e);
        }
    }

    public static boolean register(String username, String password, String email) {
        try {
            String body = String.format("{\"username\":\"%s\",\"password\":\"%s\",\"email\":\"%s\"}", username, password, email);
            // Backend returns 201 Created with UserResponseDTO body; success if no exception
            ApiClient.postJson("/api/auth/register", body);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}


