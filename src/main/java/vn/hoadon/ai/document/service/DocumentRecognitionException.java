package vn.hoadon.ai.document.service;

public class DocumentRecognitionException extends RuntimeException {
    private final String code;

    public DocumentRecognitionException(String code, String message) {
        super(message);
        this.code = code;
    }

    public DocumentRecognitionException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
