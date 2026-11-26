package vn.hoadon.ai.knowledge.service;

import org.springframework.stereotype.Component;
import vn.hoadon.util.UploadPath;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Component
public class KnowledgeDocumentStorage {
    private static final long STORAGE_COMPANY_ID = 1L;
    private static final String STORAGE_TYPE = "knowledge";

    public StoredFile write(String originalFilename, byte[] bytes) {
        String extension = extension(originalFilename);
        String filename = UUID.randomUUID() + extension;
        try {
            Path directory = UploadPath.resolveCompanyTypeDir(STORAGE_COMPANY_ID, STORAGE_TYPE);
            Path target = directory.resolve(filename).normalize();
            if (!target.startsWith(directory)) throw new IOException("Invalid knowledge document path");
            Path temporary = Files.createTempFile(directory, ".pending-", ".tmp");
            try {
                Files.write(temporary, bytes);
                try {
                    Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException ex) {
                    Files.move(temporary, target);
                }
            } finally {
                Files.deleteIfExists(temporary);
            }
            String key = UploadPath.encodeCompanyId(STORAGE_COMPANY_ID) + "/" + STORAGE_TYPE + "/" + filename;
            return new StoredFile(key, target);
        } catch (IOException ex) {
            throw new IllegalStateException("Không lưu được tài liệu thư viện AI", ex);
        }
    }

    public void delete(String key) {
        if (key == null || key.isBlank()) return;
        Path base = UploadPath.basePath();
        Path path = base.resolve(key).normalize();
        if (!path.startsWith(base)) throw new IllegalArgumentException("Đường dẫn tài liệu AI không hợp lệ");
        try {
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            throw new IllegalStateException("Không xóa được tài liệu thư viện AI", ex);
        }
    }

    private String extension(String filename) {
        if (filename == null) throw new IllegalArgumentException("Tên tài liệu không hợp lệ");
        int dot = filename.lastIndexOf('.');
        if (dot < 0) throw new IllegalArgumentException("Tài liệu phải có phần mở rộng");
        String extension = filename.substring(dot).toLowerCase(Locale.ROOT);
        if (!extension.matches("\\.[a-z0-9]{1,8}")) {
            throw new IllegalArgumentException("Phần mở rộng tài liệu không hợp lệ");
        }
        return extension;
    }

    public record StoredFile(String key, Path path) { }
}
