package vn.hoadon.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import vn.hoadon.ai.model.AiFunctionCall;
import vn.hoadon.ai.model.AiUsage;

import java.util.List;

public record AiProviderResponse(
        String text,
        JsonNode structuredOutput,
        List<AiFunctionCall> functionCalls,
        AiUsage usage,
        String model,
        String finishReason
) {
    public AiProviderResponse {
        functionCalls = functionCalls == null ? List.of() : List.copyOf(functionCalls);
        usage = usage == null ? AiUsage.empty() : usage;
    }
}
