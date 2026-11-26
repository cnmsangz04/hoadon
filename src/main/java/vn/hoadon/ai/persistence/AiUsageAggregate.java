package vn.hoadon.ai.persistence;

public interface AiUsageAggregate {
    Long getRequestCount();
    Long getSuccessCount();
    Long getFailureCount();
    Long getInputTokens();
    Long getOutputTokens();
    Long getTotalTokens();
}
