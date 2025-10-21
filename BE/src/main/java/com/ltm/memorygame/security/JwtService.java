package com.ltm.memorygame.security;

import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtService {

    @Value("${app.jwt.secret:change-me-secret}")
    private String secret;

    @Value("${app.jwt.expirationMs:86400000}")
    private long expirationMs;

    private static final Gson gson = new Gson();

    public String generateToken(Long userId, String username) {
        Map<String, Object> payload = new HashMap<>();
        long now = Instant.now().getEpochSecond();
        long exp = Instant.now().plusMillis(expirationMs).getEpochSecond();
        payload.put("sub", username);
        payload.put("userId", userId);
        payload.put("iat", now);
        payload.put("exp", exp);
        return sign(payload);
    }

    private String sign(Map<String, Object> payload) {
        String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
        String payloadJson = toJson(payload);
        String header = base64UrlEncode(headerJson.getBytes(StandardCharsets.UTF_8));
        String body = base64UrlEncode(payloadJson.getBytes(StandardCharsets.UTF_8));
        String toSign = header + "." + body;
        String signature = hmacSha256(toSign, secret);
        return toSign + "." + signature;
    }

    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            String signed = parts[0] + "." + parts[1];
            String expectedSig = hmacSha256(signed, secret);
            if (!expectedSig.equals(parts[2])) return false;

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<?,?> payload = gson.fromJson(payloadJson, Map.class);
            Object expObj = payload.get("exp");
            if (expObj instanceof Number) {
                long exp = ((Number) expObj).longValue();
                long now = Instant.now().getEpochSecond();
                return now < exp;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public String extractUsername(String token) {
        Map<String, Object> p = extractPayload(token);
        Object sub = p.get("sub");
        return sub != null ? String.valueOf(sub) : null;
    }

    public Long extractUserId(String token) {
        Map<String, Object> p = extractPayload(token);
        Object id = p.get("userId");
        if (id instanceof Number) return ((Number) id).longValue();
        try { return id != null ? Long.parseLong(String.valueOf(id)) : null; } catch (Exception e) { return null; }
    }

    private Map<String, Object> extractPayload(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return new HashMap<>();
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            Map<String, Object> map = gson.fromJson(payloadJson, Map.class);
            return map != null ? map : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return base64UrlEncode(sig);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to sign JWT", e);
        }
    }

    private static String base64UrlEncode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // Minimal JSON serializer for flat map
    private static String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            sb.append('"').append(escape(e.getKey())).append('"').append(':');
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) {
                sb.append(v.toString());
            } else {
                sb.append('"').append(escape(String.valueOf(v))).append('"');
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

