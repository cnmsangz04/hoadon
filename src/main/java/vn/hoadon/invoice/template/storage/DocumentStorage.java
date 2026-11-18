package vn.hoadon.invoice.template.storage;

import org.springframework.stereotype.Component;
import vn.hoadon.invoice.template.config.InvoiceTemplateProperties;
import vn.hoadon.invoice.template.service.TemplateException;
import vn.hoadon.util.UploadPath;

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

    public String write(byte[] bytes, Long companyId, String folder, String extension) {
        String owner = companyDirectory(companyId);
        String key = owner + "/" + folder + "/" + UUID.randomUUID() + normalizeExtension(extension);
        Path temp = null;
        try {
            Files.createDirectories(root);
            temp = Files.createTempFile(root, ".pending-", ".tmp");
            Files.write(temp, bytes);
            Files.createDirectories(resolve(root, key).getParent());
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
            Path path = resolve(root, key);
            byte[] bytes = Files.readAllBytes(path);
            if (!sha256(bytes).equals(expectedHash)) {
                throw TemplateException.invalid("ASSET_HASH_MISMATCH", "Nội dung tài liệu không khớp bản đã lưu");
            }
            return bytes;
        } catch (IOException ex) {
            throw TemplateException.invalid("ASSET_MISSING", "Không tìm thấy tài nguyên của tài liệu");
        }
    }

    public boolean delete(String key) {
        try {
            return Files.deleteIfExists(resolve(key));
        } catch (IOException ex) {
            throw new IllegalStateException("Không xóa được tài liệu", ex);
        }
    }

    private Path resolve(Path base, String key) {
        if (key == null || key.isBlank() || key.startsWith("/") || key.contains("..") || !key.matches("[A-Za-z0-9._/-]+")) {
            throw TemplateException.invalid("INVALID_STORAGE_KEY", "Định danh tài liệu không hợp lệ");
        }
        Path resolved = base.resolve(key).normalize();
        if (!resolved.startsWith(base)) throw TemplateException.invalid("INVALID_STORAGE_KEY", "Định danh tài liệu không hợp lệ");
        return resolved;
    }

    private Path resolve(String key) { return resolve(root, key); }

    private static String companyDirectory(Long companyId) {
        if (companyId == null || companyId < 0) throw TemplateException.invalid("COMPANY_REQUIRED", "Thiếu công ty của tài liệu");
        return UploadPath.encodeCompanyId(companyId);
    }

    private static String normalizeExtension(String extension) {
        if (extension == null || extension.isBlank()) return "";
        String value = extension.startsWith(".") ? extension : "." + extension;
        if (!value.matches("\\.[a-z0-9]{1,8}")) throw TemplateException.invalid("INVALID_STORAGE_KEY", "Định dạng file không hợp lệ");
        return value;
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
