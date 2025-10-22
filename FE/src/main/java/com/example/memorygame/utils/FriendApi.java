package com.example.memorygame.utils;

import com.example.memorygame.model.user.FriendListDTO;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class FriendApi {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Gets the friend list for the current logged-in user.
     * @return FriendListDTO containing friends, incoming requests, and outgoing requests
     */
    public static FriendListDTO getFriendList() {
        try {
            String token = TokenManager.getInstance().getToken();
            if (token == null) {
                return null;
            }
            
            Long userId = JwtDecoder.extractUserId(token);
            if (userId == null) {
                return null;
            }
            
            String json = ApiClient.getAuth("/api/friends/" + userId);
            return MAPPER.readValue(json, FriendListDTO.class);
        } catch (Exception e) {
            System.err.println("Failed to get friend list: " + e.getMessage());
            return null;
        }
    }
}

