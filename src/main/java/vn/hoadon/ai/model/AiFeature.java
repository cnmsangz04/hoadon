package vn.hoadon.ai.model;

import java.util.Arrays;

public enum AiFeature {
    INVOICE_ASSISTANT("invoice-assistant"),
    DOCUMENT_RECOGNITION("document-recognition"),
    CHATBOT("chatbot"),
    KNOWLEDGE_MANAGEMENT("knowledge-management");

    private final String code;

    AiFeature(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static AiFeature fromCode(String code) {
        return Arrays.stream(values())
                .filter(feature -> feature.code.equalsIgnoreCase(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Tính năng AI không hợp lệ"));
    }
}
