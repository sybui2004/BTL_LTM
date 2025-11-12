package com.example.memorygame.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.memorygame.model.game.InviteDTO;

public class InviteApi {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Get pending invites for current user
     */
    public static List<InviteDTO> getPendingInvites(Long userId) {
        try {
            String response = ApiClient.getAuth("/api/invites/pending/" + userId);
            return MAPPER.readValue(response, 
                new TypeReference<List<InviteDTO>>(){});
        } catch (Exception e) {
            System.err.println("[InviteApi] Failed to get invites: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Accept an invite
     */
    public static boolean acceptInvite(Long roomId, Long playerId) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("playerId", playerId);
            
            String json = MAPPER.writeValueAsString(request);
            ApiClient.postJsonAuth("/api/invites/accept", json);
            return true;
        } catch (Exception e) {
            System.err.println("[InviteApi] Failed to accept invite: " + e.getMessage());
            return false;
        }
    }

    /**
     * Reject an invite
     */
    public static boolean rejectInvite(Long roomId, Long playerId) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("playerId", playerId);
            
            String json = MAPPER.writeValueAsString(request);
            ApiClient.postJsonAuth("/api/invites/reject", json);
            return true;
        } catch (Exception e) {
            System.err.println("[InviteApi] Failed to reject invite: " + e.getMessage());
            return false;
        }
    }
}

