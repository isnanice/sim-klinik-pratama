package com.klinik.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

/**
 * Utilitas untuk melakukan hashing dan verifikasi password menggunakan SHA-256 + random salt (SecureRandom).
 * Format penyimpanan: "salt:hash" dalam representasi heksadesimal.
 */
public final class PasswordUtil {
    private static final int SALT_LENGTH_BYTES = 16;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /**
     * Menghasilkan hash password dengan format "salt:hash" menggunakan SHA-256 dan SecureRandom salt.
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("Password tidak boleh kosong.");
        }
        byte[] saltBytes = new byte[SALT_LENGTH_BYTES];
        SECURE_RANDOM.nextBytes(saltBytes);
        String saltHex = bytesToHex(saltBytes);
        String hashHex = sha256Hex(saltHex, plainPassword);
        return saltHex + ":" + hashHex;
    }

    /**
     * Alias untuk hashPassword(String).
     */
    public static String hash(String plainPassword) {
        return hashPassword(plainPassword);
    }

    /**
     * Memverifikasi kecocokan plainPassword dengan storedPassword berformat "salt:hash".
     */
    public static boolean verifyPassword(String plainPassword, String storedPassword) {
        if (plainPassword == null || storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        int sepIdx = storedPassword.indexOf(':');
        if (sepIdx <= 0 || sepIdx == storedPassword.length() - 1) {
            // Fallback jika data lama belum di-hash dengan salt:hash
            return MessageDigest.isEqual(
                    plainPassword.getBytes(StandardCharsets.UTF_8),
                    storedPassword.getBytes(StandardCharsets.UTF_8)
            );
        }
        String saltHex = storedPassword.substring(0, sepIdx);
        String expectedHashHex = storedPassword.substring(sepIdx + 1);
        String actualHashHex = sha256Hex(saltHex, plainPassword);
        return MessageDigest.isEqual(
                expectedHashHex.toLowerCase().getBytes(StandardCharsets.UTF_8),
                actualHashHex.toLowerCase().getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * Alias untuk verifyPassword(String, String).
     */
    public static boolean verify(String plainPassword, String storedPassword) {
        return verifyPassword(plainPassword, storedPassword);
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
