package vn.hoadon.ai.provider;

public class AiProviderException extends RuntimeException {
    private final String code;
    private final boolean retryable;
    private final long retryAfterMs;

    public AiProviderException(String code, String message, boolean retryable) {
        this(code, message, retryable, 0, null);
    }

    public AiProviderException(String code, String message, boolean retryable, long retryAfterMs, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.retryable = retryable;
        this.retryAfterMs = Math.max(0, retryAfterMs);
    }

    public String getCode() { return code; }
    public boolean isRetryable() { return retryable; }
    public long getRetryAfterMs() { return retryAfterMs; }
}
