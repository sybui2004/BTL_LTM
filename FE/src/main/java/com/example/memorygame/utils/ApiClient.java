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
}


