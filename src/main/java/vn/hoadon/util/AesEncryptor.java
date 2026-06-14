package vn.hoadon.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for sensitive fields (e.g. SMTP passwords).
 *
 * Encrypted format (Base64): [12-byte IV][ciphertext+16-byte auth tag]
 *
 * The encryption key is derived from {@code app.encryption.key} in
 * application.properties (must be 32-character hex or plain string padded to 32 bytes).
 */
@Component
public class AesEncryptor {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    IV_BYTES   = 12;   // 96-bit IV recommended for GCM
    private static final int    TAG_BITS   = 128;  // authentication tag length

    private final SecretKey secretKey;

    public AesEncryptor(@Value("${app.encryption.key:DefaultEncryptionKey1234567890ABCD}") String rawKey) {
        byte[] keyBytes = prepareKey(rawKey);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /** Encrypt plaintext. Returns Base64-encoded [IV + ciphertext]. */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return plaintext;
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Prepend IV to ciphertext
            byte[] combined = new byte[IV_BYTES + encrypted.length];
            System.arraycopy(iv,        0, combined, 0,        IV_BYTES);
            System.arraycopy(encrypted, 0, combined, IV_BYTES, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    /** Decrypt a value produced by {@link #encrypt(String)}. */
    public String decrypt(String cipherBase64) {
        if (cipherBase64 == null || cipherBase64.isBlank()) return cipherBase64;
        try {
            byte[] combined = Base64.getDecoder().decode(cipherBase64);
            if (combined.length <= IV_BYTES) throw new IllegalArgumentException("Invalid ciphertext");

            byte[] iv         = new byte[IV_BYTES];
            byte[] ciphertext = new byte[combined.length - IV_BYTES];
            System.arraycopy(combined, 0,        iv,         0, IV_BYTES);
            System.arraycopy(combined, IV_BYTES, ciphertext, 0, ciphertext.length);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] decrypted = cipher.doFinal(ciphertext);

            return new String(decrypted, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("AES decryption failed", e);
        }
    }

    // Pad or truncate rawKey to exactly 32 bytes for AES-256
    private byte[] prepareKey(String rawKey) {
        byte[] src = rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        System.arraycopy(src, 0, key, 0, Math.min(src.length, 32));
        return key;
    }
}
