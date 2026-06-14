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
 * Tiện ích mã hóa AES-256-GCM cho các trường nhạy cảm, ví dụ mật khẩu SMTP.
 *
 * Định dạng sau mã hóa (Base64): [IV 12 byte][dữ liệu mã hóa + thẻ xác thực 16 byte]
 *
 * Khóa mã hóa được lấy từ {@code app.encryption.key} trong
 * application.properties. Khóa cần là chuỗi hex 32 ký tự hoặc chuỗi thường được đệm đến 32 byte.
 */
@Component
public class AesEncryptor {

    private static final String ALGORITHM  = "AES/GCM/NoPadding";
    private static final int    IV_BYTES   = 12;   // IV 96-bit được khuyến nghị cho GCM
    private static final int    TAG_BITS   = 128;  // độ dài auth tag

    private final SecretKey secretKey;

    public AesEncryptor(@Value("${app.encryption.key:DefaultEncryptionKey1234567890ABCD}") String rawKey) {
        byte[] keyBytes = prepareKey(rawKey);
        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    /** Mã hóa văn bản thường. Trả về chuỗi Base64 dạng [IV + ciphertext]. */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isBlank()) return plaintext;
        try {
            byte[] iv = new byte[IV_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // Gắn IV vào trước dữ liệu đã mã hóa.
            byte[] combined = new byte[IV_BYTES + encrypted.length];
            System.arraycopy(iv,        0, combined, 0,        IV_BYTES);
            System.arraycopy(encrypted, 0, combined, IV_BYTES, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("AES encryption failed", e);
        }
    }

    /** Giải mã giá trị được tạo bởi {@link #encrypt(String)}. */
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

    // Đệm hoặc cắt rawKey về đúng 32 byte cho AES-256.
    private byte[] prepareKey(String rawKey) {
        byte[] src = rawKey.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] key = new byte[32];
        System.arraycopy(src, 0, key, 0, Math.min(src.length, 32));
        return key;
    }
}
