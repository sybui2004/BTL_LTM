package com.example.memorygame.utils;

import com.example.memorygame.model.game.RoomResponseDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RoomApi {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Create a new room for the current user
     */
    public static RoomResponseDTO createRoom(Long hostId, Long guestId) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("hostId", hostId);
            if (guestId != null) {
                request.put("guestId", guestId);
            }

            String json = MAPPER.writeValueAsString(request);
            String response = ApiClient.postJsonAuth("/api/rooms", json);
            return MAPPER.readValue(response, RoomResponseDTO.class);
        } catch (Exception e) {
            System.err.println("[RoomApi] Failed to create room: " + e.getMessage());
            return null;
        }
    }

    /**
     * Send invite to a friend
     */
    public static boolean sendInvite(Long roomId, Long senderId, Long targetId) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("senderId", senderId);
            request.put("targetId", targetId);

            String json = MAPPER.writeValueAsString(request);
            ApiClient.postJsonAuth("/api/invites/send", json);
            return true;
        } catch (Exception e) {
            System.err.println("[RoomApi] Failed to send invite: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get all waiting rooms (to find if user already has a room)
     */
    public static java.util.List<RoomResponseDTO> getWaitingRooms() {
        try {
            String response = ApiClient.getAuth("/api/rooms");
            return MAPPER.readValue(response, new TypeReference<List<RoomResponseDTO>>() {
            });
        } catch (Exception e) {
            System.err.println("[RoomApi] Failed to get rooms: " + e.getMessage());
            return java.util.Collections.emptyList();
        }
    }

    /**
     * Exit a room (host or guest).
     * This is safe to call even if backend already processed exit via TCP.
     */
    public static boolean exitRoom(Long roomId, Long playerId) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("roomId", roomId);
            request.put("playerId", playerId);
            String json = MAPPER.writeValueAsString(request);
            ApiClient.postJsonAuth("/api/rooms/exit", json);
            return true;
        } catch (Exception e) {
            System.err.println("[RoomApi] Failed to exit room: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get a specific room by ID
     */
    public static RoomResponseDTO getRoom(Long roomId) {
        try {
            String response = ApiClient.getAuth("/api/rooms/" + roomId);
            return MAPPER.readValue(response, RoomResponseDTO.class);
        } catch (Exception e) {
            System.err.println("[RoomApi] Failed to get room: " + e.getMessage());
            return null;
        }
    }
}
