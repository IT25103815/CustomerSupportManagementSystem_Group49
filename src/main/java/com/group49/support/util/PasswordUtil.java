package com.group49.support.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

public final class PasswordUtil {
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {}

    public static String hash(String password) {
        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);
        return ITERATIONS + ":" + HexFormat.of().formatHex(salt) + ":" +
                HexFormat.of().formatHex(derive(password.toCharArray(), salt, ITERATIONS));
    }

    public static boolean verify(String password, String stored) {
        try {
            String[] parts = stored.split(":");
            int iterations = Integer.parseInt(parts[0]);
            byte[] salt = HexFormat.of().parseHex(parts[1]);
            byte[] expected = HexFormat.of().parseHex(parts[2]);
            byte[] actual = derive(password.toCharArray(), salt, iterations);
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static boolean isStrong(String password) {
        if (password == null || password.length() < 8) return false;
        boolean upper = false, lower = false, digit = false, special = false;
        for (char ch : password.toCharArray()) {
            if (Character.isUpperCase(ch)) upper = true;
            else if (Character.isLowerCase(ch)) lower = true;
            else if (Character.isDigit(ch)) digit = true;
            else if (!Character.isWhitespace(ch)) special = true;
        }
        return upper && lower && digit && special;
    }

    public static String requirements() {
        return "Use at least 8 characters with uppercase, lowercase, a number and a special character.";
    }

    public static String hashToken(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Token hashing failed", exception);
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_LENGTH);
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
        } catch (Exception exception) {
            throw new IllegalStateException("Password hashing failed", exception);
        }
    }
}
