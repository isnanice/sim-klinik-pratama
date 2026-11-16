package com.klinik.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Utilitas untuk melakukan hashing dan verifikasi password menggunakan SHA-256 + random salt (SecureRandom).
 * Format penyimpanan utama: Base64(salt) + ":" + Base64(SHA-256(salt + password)) sesuai sql/schema.sql dan sql/seed.sql.
 * Tetap mendukung verifikasi mundur untuk format heksadesimal.
 */
public final class PasswordUtil {
    private static final int SALT_LENGTH_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /**
     * Menghasilkan hash password dengan format "Base64(salt):Base64(hash)" menggunakan SHA-256 dan SecureRandom salt.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password tidak boleh kosong.");
        }
        byte[] saltBytes = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(saltBytes);
        byte[] hashBytes = sha256Bytes(saltBytes, plainPassword);
        return Base64.getEncoder().encodeToString(saltBytes) + ":" + Base64.getEncoder().encodeToString(hashBytes);
    }

    /**
     * Alias untuk hashPassword(String).
     */
    public static String hash(String plainPassword) {
        return hashPassword(plainPassword);
    }

    /**
     * Memverifikasi kecocokan plainPassword dengan storedPassword berformat "salt:hash" (Base64 maupun Hex).
     */
    public static boolean verifyPassword(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        int sepIdx = storedPassword.indexOf(':');
        if (sepIdx <= 0 || sepIdx == storedPassword.length() - 1) {
            return MessageDigest.isEqual(
                    plainPassword.getBytes(StandardCharsets.UTF_8),
                    storedPassword.getBytes(StandardCharsets.UTF_8)
            );
        }
        String saltPart = storedPassword.substring(0, sepIdx);
        String expectedHashPart = storedPassword.substring(sepIdx + 1);

        // 1. Verifikasi format Base64 sesuai sql/schema.sql & sql/seed.sql: Base64(salt):Base64(SHA-256(saltBytes + password))
        try {
            byte[] saltBytes = Base64.getDecoder().decode(saltPart);
            byte[] expectedHashBytes = Base64.getDecoder().decode(expectedHashPart);
            if (expectedHashBytes.length == 32) {
                byte[] actualHashBytes = sha256Bytes(saltBytes, plainPassword);
                if (MessageDigest.isEqual(expectedHashBytes, actualHashBytes)) {
                    return true;
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Lanjut ke fallback Hex
        }

        // 2. Fallback format Hex lama
        String actualHashHex = sha256Hex(saltPart, plainPassword);
        return MessageDigest.isEqual(
                expectedHashPart.toLowerCase().getBytes(StandardCharsets.UTF_8),
                actualHashHex.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Alias untuk verifyPassword(String, String).
     */
    public static boolean verify(String plainPassword, String storedPassword) {
        return verifyPassword(plainPassword, storedPassword);
    }

    private static byte[] sha256Bytes(byte[] saltBytes, String plainPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(saltBytes);
            return md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritma SHA-256 tidak tersedia pada JVM ini.", e);
        }
    }

    private static String sha256Hex(String saltHex, String plainPassword) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(saltHex.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritma SHA-256 tidak tersedia pada JVM ini.", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
