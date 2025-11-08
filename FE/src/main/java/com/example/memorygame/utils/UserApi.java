package com.example.memorygame.utils;

import com.example.memorygame.model.user.UserSummary;
import com.example.memorygame.model.user.UserProfileDTO;
import com.example.memorygame.model.user.UserResponseDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserApi {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static List<UserSummary> getRankingUsers() {
        try {
            String json = ApiClient.getAuth("/api/users/ranking");
            return MAPPER.readValue(json, new TypeReference<List<UserSummary>>() {
            });
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static List<UserSummary> getRecentPlayers() {
        try {
            String json = ApiClient.getAuth("/api/users/recent");
            return MAPPER.readValue(json, new TypeReference<List<UserSummary>>(){});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static UserSummary getUserById(long id) {
        try {
            String json = ApiClient.getAuth("/api/users/" + id);
            UserSummary user = MAPPER.readValue(json, UserSummary.class);
            // Guard against parsing error-body into an empty UserSummary
            if ((user.username == null && user.displayName == null) && user.id == 0) {
                return null;
            }
            return user;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Gets full user details including email
     * 
     * @param id User ID
     * @return UserResponseDTO or null if error
     */
    public static UserResponseDTO getUserDetailsById(long id) {
        try {
            String json = ApiClient.getAuth("/api/users/" + id);
            UserResponseDTO user = MAPPER.readValue(json, UserResponseDTO.class);
            if (user != null && user.id == null) {
                return null;
            }
            return user;
        } catch (Exception e) {
            System.err.println("Failed to get user details: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the current logged-in user's information by extracting their ID from the
     * JWT token.
     * 
     * @return UserSummary of current user or null if not logged in or error occurs
     */
    public static UserSummary getCurrentUser() {
        try {
            String token = TokenManager.getInstance().getToken();
            if (token == null) {
                return null;
            }

            Long userId = JwtDecoder.extractUserId(token);
            if (userId == null) {
                return null;
            }

            return getUserById(userId);
        } catch (Exception e) {
            System.err.println("Failed to get current user: " + e.getMessage());
            return null;
        }
    }

    /**
     * Gets the ranking/leaderboard with detailed stats
     * 
     * @return List of maps containing user ranking data
     */
    public static List<Map<String, Object>> getRanking() {
        try {
            String json = ApiClient.getAuth("/api/users/ranking");
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            System.err.println("Failed to get ranking: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Gets all users as Map objects
     * 
     * @return List of user data maps
     */
    public static List<Map<String, Object>> getAllUsers() {
        try {
            String json = ApiClient.getAuth("/api/users");
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            System.err.println("Failed to get all users: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Gets all users as strongly-typed UserSummary objects
     *
     * @return List of UserSummary
     */
    public static List<UserSummary> getAllUserSummaries() {
        try {
            String json = ApiClient.getAuth("/api/users");
            return MAPPER.readValue(json, new TypeReference<List<UserSummary>>() {
            });
        } catch (Exception e) {
            System.err.println("Failed to get all user summaries: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Search users by query
     * 
     * @param query Search query
     * @return List of matching user data maps
     */
    public static List<Map<String, Object>> searchUsers(String query) {
        try {
            String json = ApiClient.getAuth("/api/users/search?q=" + java.net.URLEncoder.encode(query, "UTF-8"));
            return MAPPER.readValue(json, new TypeReference<List<Map<String, Object>>>() {
            });
        } catch (Exception e) {
            System.err.println("Failed to search users: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Gets user profile with match history (top 20)
     * 
     * @param userId User ID
     * @return UserProfileDTO or null if error
     */
    public static UserProfileDTO getUserProfile(Long userId) {
        try {
            String json = ApiClient.getAuth("/api/users/" + userId + "/profile");
            return MAPPER.readValue(json, UserProfileDTO.class);
        } catch (Exception e) {
            System.err.println("Failed to get user profile: " + e.getMessage());
            return null;
        }
    }

    /**
     * Updates user profile (display name and avatar)
     * 
     * @param userId User ID
     * @param displayName New display name
     * @param avatarUrl New avatar URL
     * @return true if successful, false otherwise
     */
    public static boolean updateProfile(Long userId, String displayName, String avatarUrl) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("displayName", displayName);
            body.put("avatarUrl", avatarUrl);
            
            String jsonBody = MAPPER.writeValueAsString(body);
            String response = ApiClient.patchAuth("/api/users/" + userId, jsonBody);
            return response != null && !response.isEmpty();
        } catch (Exception e) {
            System.err.println("Failed to update profile: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Changes user password
     * 
     * @param userId User ID
     * @param newPassword New password
     * @return true if successful, false otherwise
     */
    public static boolean changePassword(Long userId, String newPassword) {
        try {
            Map<String, String> body = new HashMap<>();
            body.put("password", newPassword);
            
            String jsonBody = MAPPER.writeValueAsString(body);
            ApiClient.patchAuth("/api/users/" + userId + "/password", jsonBody);
            return true;
        } catch (Exception e) {
            System.err.println("Failed to change password: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets user Elo (totalScore) from ranking
     * 
     * @param userId User ID
     * @return Elo score or 0 if not found
     */
    public static Integer getUserElo(Long userId) {
        try {
            List<Map<String, Object>> ranking = getRanking();
            for (Map<String, Object> entry : ranking) {
                Object idObj = entry.get("id");
                if (idObj != null) {
                    Long id = ((Number) idObj).longValue();
                    if (id.equals(userId)) {
                        Object scoreObj = entry.get("totalScore");
                        if (scoreObj != null) {
                            return ((Number) scoreObj).intValue();
                        }
                    }
                }
            }
            return 0;
        } catch (Exception e) {
            System.err.println("Failed to get user Elo: " + e.getMessage());
            return 0;
        }
    }
}
