package vn.hoadon.ai.model;

public record AiUsage(long inputTokens, long outputTokens, long totalTokens) {
    public static AiUsage empty() {
        return new AiUsage(0, 0, 0);
    }
}
