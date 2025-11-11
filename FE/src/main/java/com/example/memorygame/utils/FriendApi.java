package com.example.memorygame.utils;

import com.example.memorygame.model.user.FriendListDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class FriendApi {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /**
     * Gets the friend list for the current logged-in user.
     * 
     * @return FriendListDTO containing friends, incoming requests, and outgoing
     *         requests
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

    /**
     * Gets list of friends (accepted friendships only) as Map objects
     * 
     * @return List of friend data maps
     */
    public static List<Map<String, Object>> getFriends() {
        try {
            FriendListDTO friendList = getFriendList();
            if (friendList == null || friendList.friends == null) {
                return Collections.emptyList();
            }

            // Convert to List<Map<String, Object>>
            String json = MAPPER.writeValueAsString(friendList.friends);
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            System.err.println("Failed to get friends: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Gets list of pending friend requests (incoming)
     * 
     * @return List of friend request data maps
     */
    public static List<Map<String, Object>> getPendingRequests() {
        try {
            FriendListDTO friendList = getFriendList();
            if (friendList == null || friendList.incomingRequest == null) {
                return Collections.emptyList();
            }

            // Convert to List<Map<String, Object>>
            String json = MAPPER.writeValueAsString(friendList.incomingRequest);
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            System.err.println("Failed to get pending requests: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Accepts a friend request
     * 
     * @param requestId The ID of the friend request
     * @return true if successful
     */
    public static boolean acceptFriendRequest(Long requestId) {
        try {
            String token = TokenManager.getInstance().getToken();
            if (token == null)
                return false;

            Long userId = JwtDecoder.extractUserId(token);
            if (userId == null)
                return false;

            String endpoint = String.format("/api/friends/%d/requests/%d/accept", userId, requestId);
            ApiClient.postJsonAuth(endpoint, "{}");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to accept friend request: " + e.getMessage());
            return false;
        }
    }

    /**
     * Rejects a friend request
     * 
     * @param requestId The ID of the friend request
     * @return true if successful
     */
    public static boolean rejectFriendRequest(Long requestId) {
        try {
            String token = TokenManager.getInstance().getToken();
            if (token == null)
                return false;

            Long userId = JwtDecoder.extractUserId(token);
            if (userId == null)
                return false;

            String endpoint = String.format("/api/friends/%d/requests/%d/reject", userId, requestId);
            ApiClient.postJsonAuth(endpoint, "{}");
            return true;
        } catch (Exception e) {
            System.err.println("Failed to reject friend request: " + e.getMessage());
            return false;
        }
    }

    /**
     * Sends a friend request to target user
     *
     * @param currentUserId The sender id (must match JWT)
     * @param targetUserId  The receiver id
     * @return true if successful
     */
    public static boolean sendFriendRequest(Long currentUserId, Long targetUserId) {
        try {
            String token = TokenManager.getInstance().getToken();
            if (token == null)
                return false;

            Long userId = JwtDecoder.extractUserId(token);
            if (userId == null || !userId.equals(currentUserId))
                return false;

            String endpoint = String.format("/api/friends/%d/requests", currentUserId);
            String body = String.format("{\"toUserId\":%d}", targetUserId);
            ApiClient.postJsonAuth(endpoint, body);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to send friend request: " + e.getMessage());
            return false;
        }
    }

    /**
     * Removes a friend (unfriends)
     *
     * @param currentUserId The current user id (must match JWT)
     * @param friendUserId   The friend user id to remove
     * @return true if successful
     */
    public static boolean removeFriend(Long currentUserId, Long friendUserId) {
        try {
            String token = TokenManager.getInstance().getToken();
            if (token == null)
                return false;

            Long userId = JwtDecoder.extractUserId(token);
            if (userId == null || !userId.equals(currentUserId))
                return false;

            String endpoint = String.format("/api/friends/%d/%d", currentUserId, friendUserId);
            ApiClient.deleteAuth(endpoint);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to remove friend: " + e.getMessage());
            return false;
        }
    }
}
