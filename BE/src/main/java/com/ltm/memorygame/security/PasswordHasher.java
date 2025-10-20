package com.ltm.memorygame.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordHasher {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32; // 256-bit
    private static final int ITERATIONS = 100_000;

    public String hashPassword(String rawPassword) {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS, HASH_BYTES);

        String saltB64 = Base64.getEncoder().encodeToString(salt);
        String hashB64 = Base64.getEncoder().encodeToString(hash);
        return String.format("PBKDF2$%d$%s$%s", ITERATIONS, saltB64, hashB64);
    }

    public boolean verifyPassword(String rawPassword, String stored) {
        if (stored == null || rawPassword == null) return false;
        String[] parts = stored.split("\\$");
        if (parts.length == 4 && parts[0].equals("PBKDF2")) {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] actual = pbkdf2(rawPassword.toCharArray(), salt, iterations, expectedHash.length);
            return MessageDigest.isEqual(expectedHash, actual);
        }
        // Backward-compatibility: if stored is plaintext, fall back to direct compare
        return rawPassword.equals(stored);
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int lengthBytes) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, lengthBytes * 8);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash password", e);
        }
    }
}


