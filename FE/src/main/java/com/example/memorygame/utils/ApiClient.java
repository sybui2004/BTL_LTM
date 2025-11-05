package com.example.memorygame.utils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiClient {
    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();
    private static String baseUrl = "http://localhost:8080";

    public static void setBaseUrl(String url) {
        baseUrl = url;
    }

    public static String get(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .GET()
                    .build();
			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			int status = response.statusCode();
			if (status >= 200 && status < 300) {
				return response.body();
			}
			throw new RuntimeException("GET failed (" + status + ") for " + path + ": " + response.body());
        } catch (Exception e) {
            throw new RuntimeException("GET request failed: " + path, e);
        }
    }

    public static String getAuth(String path) {
        try {
            String token = TokenManager.getInstance().getToken();
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("Missing auth token");
            }
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();
			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			int status = response.statusCode();
			if (status >= 200 && status < 300) {
				return response.body();
			}
			throw new RuntimeException("GET (auth) failed (" + status + ") for " + path + ": " + response.body());
        } catch (Exception e) {
            throw new RuntimeException("GET (auth) request failed: " + path, e);
        }
    }

    public static String postJson(String path, String jsonBody) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			int status = response.statusCode();
			if (status >= 200 && status < 300) {
				return response.body();
			}
			throw new RuntimeException("POST failed (" + status + ") for " + path + ": " + response.body());
        } catch (Exception e) {
            throw new RuntimeException("POST request failed: " + path, e);
        }
    }

    public static String postJsonAuth(String path, String jsonBody) {
        try {
            String token = TokenManager.getInstance().getToken();
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("Missing auth token");
            }
            System.out.println("[ApiClient] POST " + path + " with auth");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			int status = response.statusCode();
			if (status >= 200 && status < 300) {
				return response.body();
			}
			System.err.println("[ApiClient] POST failed - Status: " + status + ", Body: " + response.body());
			throw new RuntimeException("POST (auth) failed (" + status + ") for " + path + ": " + response.body());
        } catch (Exception e) {
            System.err.println("[ApiClient] Exception: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("POST (auth) request failed: " + path, e);
        }
    }

    public static void deleteAuth(String path) {
        try {
            String token = TokenManager.getInstance().getToken();
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("Missing auth token");
            }
            System.out.println("[ApiClient] DELETE " + path + " with auth");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Authorization", "Bearer " + token)
                    .DELETE()
                    .build();
			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			int status = response.statusCode();
			if (status >= 200 && status < 300) {
				return;
			}
			System.err.println("[ApiClient] DELETE failed - Status: " + status + ", Body: " + response.body());
			throw new RuntimeException("DELETE (auth) failed (" + status + ") for " + path + ": " + response.body());
        } catch (Exception e) {
            System.err.println("[ApiClient] Exception: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("DELETE (auth) request failed: " + path, e);
        }
    }
    
    public static String patchAuth(String path, String jsonBody) {
        try {
            String token = TokenManager.getInstance().getToken();
            if (token == null || token.isBlank()) {
                throw new IllegalStateException("Missing auth token");
            }
            System.out.println("[ApiClient] PATCH " + path + " with auth");
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + token)
                    .method("PATCH", HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();
			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
			int status = response.statusCode();
			if (status >= 200 && status < 300) {
				return response.body();
			}
			System.err.println("[ApiClient] PATCH failed - Status: " + status + ", Body: " + response.body());
			throw new RuntimeException("PATCH (auth) failed (" + status + ") for " + path + ": " + response.body());
        } catch (Exception e) {
            System.err.println("[ApiClient] Exception: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("PATCH (auth) request failed: " + path, e);
        }
    }
}


