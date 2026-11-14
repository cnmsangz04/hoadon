package vn.hoadon.invoice.template.storage;

import org.springframework.stereotype.Component;
import vn.hoadon.invoice.template.config.InvoiceTemplateProperties;
import vn.hoadon.invoice.template.service.TemplateException;

import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class DocumentStorage {
    private final Path root;

    public DocumentStorage(InvoiceTemplateProperties properties) {
        root = Path.of(properties.getStorageRoot()).toAbsolutePath().normalize();
    }

    public String write(byte[] bytes) {
        String key = UUID.randomUUID().toString();
        Path temp = null;
        try {
            Files.createDirectories(root);
            temp = Files.createTempFile(root, ".pending-", ".tmp");
            Files.write(temp, bytes);
            Files.move(temp, resolve(key), StandardCopyOption.ATOMIC_MOVE);
            return key;
        } catch (IOException ex) {
            throw new IllegalStateException("Không lưu được tài liệu", ex);
        } finally {
            if (temp != null) {
                try { Files.deleteIfExists(temp); } catch (IOException ignored) { }
            }
        }
    }

    public byte[] read(String key, String expectedHash) {
        try {
            byte[] bytes = Files.readAllBytes(resolve(key));
            if (!sha256(bytes).equals(expectedHash)) {
                throw TemplateException.invalid("ASSET_HASH_MISMATCH", "Nội dung tài liệu không khớp bản đã lưu");
            }
            return bytes;
        } catch (IOException ex) {
            throw TemplateException.invalid("ASSET_MISSING", "Không tìm thấy tài nguyên của tài liệu");
        }
    }

    private Path resolve(String key) {
        if (key == null || !key.matches("[0-9a-f-]{36}")) {
            throw TemplateException.invalid("INVALID_STORAGE_KEY", "Định danh tài liệu không hợp lệ");
        }
        return root.resolve(key);
    }

    public static String sha256(String text) {
        return sha256(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
