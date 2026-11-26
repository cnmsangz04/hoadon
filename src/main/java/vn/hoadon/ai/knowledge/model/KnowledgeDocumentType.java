package vn.hoadon.ai.knowledge.model;

import java.util.Locale;

public enum KnowledgeDocumentType {
    MD, TXT, PDF, DOCX, XLSX, XML, XSD;

    public static KnowledgeDocumentType fromFilename(String filename) {
        if (filename == null || filename.isBlank() || !filename.contains(".")) {
            throw new IllegalArgumentException("Không xác định được định dạng tài liệu");
        }
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toUpperCase(Locale.ROOT);
        try {
            return valueOf(extension);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Chỉ hỗ trợ MD, TXT, PDF, DOCX, XLSX, XML và XSD");
        }
    }
}
