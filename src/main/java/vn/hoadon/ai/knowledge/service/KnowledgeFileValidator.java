package vn.hoadon.ai.knowledge.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.ai.knowledge.config.KnowledgeProperties;
import vn.hoadon.ai.knowledge.model.KnowledgeDocumentType;

import java.io.IOException;
import java.util.Locale;

@Component
public class KnowledgeFileValidator {
    private final KnowledgeProperties properties;

    public KnowledgeFileValidator(KnowledgeProperties properties) {
        this.properties = properties;
    }

    public ValidatedFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("Vui lòng chọn tài liệu");
        if (file.getSize() > properties.getMaxUploadBytes()) {
            throw new IllegalArgumentException("Tài liệu vượt quá giới hạn dung lượng");
        }
        String filename = safeFilename(file.getOriginalFilename());
        KnowledgeDocumentType type = KnowledgeDocumentType.fromFilename(filename);
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("Không thể đọc tài liệu tải lên", ex);
        }
        if (bytes.length > properties.getMaxUploadBytes()) {
            throw new IllegalArgumentException("Tài liệu vượt quá giới hạn dung lượng");
        }
        validateSignature(type, bytes);
        return new ValidatedFile(filename, normalizeMime(file.getContentType()), type, bytes);
    }

    public String safeFilename(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Tên tài liệu không hợp lệ");
        String filename = value.trim();
        String lower = filename.toLowerCase(Locale.ROOT);
        if (filename.length() > 255 || filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new IllegalArgumentException("Tên tài liệu không an toàn");
        }
        if (lower.equals(".env") || lower.equals("ai_implementation.md")) {
            throw new IllegalArgumentException("Tài liệu này không được phép đưa vào thư viện tài liệu");
        }
        return filename;
    }

    private void validateSignature(KnowledgeDocumentType type, byte[] bytes) {
        boolean pdf = bytes.length >= 5 && bytes[0] == '%' && bytes[1] == 'P' && bytes[2] == 'D'
                && bytes[3] == 'F' && bytes[4] == '-';
        boolean zip = bytes.length >= 4 && bytes[0] == 'P' && bytes[1] == 'K'
                && (bytes[2] == 3 || bytes[2] == 5 || bytes[2] == 7)
                && (bytes[3] == 4 || bytes[3] == 6 || bytes[3] == 8);
        if (type == KnowledgeDocumentType.PDF && !pdf) {
            throw new IllegalArgumentException("Nội dung file không phải PDF hợp lệ");
        }
        if ((type == KnowledgeDocumentType.DOCX || type == KnowledgeDocumentType.XLSX) && !zip) {
            throw new IllegalArgumentException("Nội dung file Office không hợp lệ");
        }
        if ((type == KnowledgeDocumentType.MD || type == KnowledgeDocumentType.TXT
                || type == KnowledgeDocumentType.XML || type == KnowledgeDocumentType.XSD)
                && containsNullByte(bytes)) {
            throw new IllegalArgumentException("File văn bản chứa dữ liệu nhị phân không hợp lệ");
        }
    }

    private boolean containsNullByte(byte[] bytes) {
        for (byte value : bytes) if (value == 0) return true;
        return false;
    }

    private String normalizeMime(String value) {
        return value == null || value.isBlank() ? "application/octet-stream" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record ValidatedFile(String filename, String mimeType, KnowledgeDocumentType type, byte[] bytes) {
    }
}
