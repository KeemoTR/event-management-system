package eventsystem.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {

    private static final String PREFIX = "pbkdf2_sha256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int SHA_256_HEX_LENGTH = 64;

    private PasswordUtil() {
    }

    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required.");
        }

        try {
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            new SecureRandom().nextBytes(salt);

            byte[] hash = pbkdf2(
                    plainPassword.toCharArray(),
                    salt,
                    ITERATIONS,
                    KEY_LENGTH
            );

            return PREFIX + "$"
                    + ITERATIONS + "$"
                    + Base64.getEncoder().encodeToString(salt) + "$"
                    + Base64.getEncoder().encodeToString(hash);

        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("Could not hash password.", e);
        }
    }

    public static boolean verifyPassword(String plainPassword, String storedPasswordHash) {
        if (plainPassword == null || storedPasswordHash == null || storedPasswordHash.trim().isEmpty()) {
            return false;
        }

        String stored = storedPasswordHash.trim();

        if (stored.startsWith(PREFIX + "$")) {
            return verifyPbkdf2(plainPassword, stored);
        }

        // Compatibility with old users created by the previous SHA-256 AuthService.
        if (looksLikeSha256Hex(stored)) {
            String candidate = sha256Hex(plainPassword.trim());
            return constantTimeEquals(candidate, stored);
        }

        // Compatibility with old seed/test users where password_hash = plain text.
        return constantTimeEquals(plainPassword, stored);
    }

    private static boolean verifyPbkdf2(String plainPassword, String storedPasswordHash) {
        try {
            String[] parts = storedPasswordHash.split("\\$");

            if (parts.length != 4) {
                return false;
            }

            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

            byte[] actualHash = pbkdf2(
                    plainPassword.toCharArray(),
                    salt,
                    iterations,
                    expectedHash.length * 8
            );

            return MessageDigest.isEqual(expectedHash, actualHash);

        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLength)
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        return factory.generateSecret(spec).getEncoded();
    }

    private static boolean looksLikeSha256Hex(String value) {
        return value != null && value.matches("^[a-fA-F0-9]{" + SHA_256_HEX_LENGTH + "}$");
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            StringBuilder builder = new StringBuilder();
            for (byte b : hashedBytes) {
                builder.append(String.format("%02x", b));
            }

            return builder.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available.", e);
        }
    }

    private static boolean constantTimeEquals(String first, String second) {
        if (first == null || second == null) {
            return false;
        }

        return MessageDigest.isEqual(
                first.getBytes(StandardCharsets.UTF_8),
                second.getBytes(StandardCharsets.UTF_8)
        );
    }
}