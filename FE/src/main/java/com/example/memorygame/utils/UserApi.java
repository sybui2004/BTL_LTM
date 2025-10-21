package com.example.memorygame.utils;

import com.example.memorygame.model.user.UserSummary;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

public class UserApi {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static List<UserSummary> getRankingUsers() {
        try {
            String json = ApiClient.getAuth("/api/users/ranking");
            return MAPPER.readValue(json, new TypeReference<List<UserSummary>>(){});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    public static List<UserSummary> getAllUsers() {
        try {
            String json = ApiClient.getAuth("/api/users");
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
}


