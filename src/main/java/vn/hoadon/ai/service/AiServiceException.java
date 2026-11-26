package vn.hoadon.ai.service;

public class AiServiceException extends RuntimeException {
    private final String code;

    public AiServiceException(String code, String message) {
        this(code, message, null);
    }

    public AiServiceException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
