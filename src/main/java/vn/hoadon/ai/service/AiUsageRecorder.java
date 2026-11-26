package vn.hoadon.ai.service;

import vn.hoadon.ai.model.AiExecutionContext;
import vn.hoadon.ai.provider.AiProviderResponse;

public interface AiUsageRecorder {
    void success(String requestId, AiExecutionContext context, String provider, AiProviderResponse response,
                 int inputCharacters, long durationMs, int attempts);

    void failure(String requestId, AiExecutionContext context, String provider, String model, String errorCode,
                 int inputCharacters, long durationMs, int attempts);
}
