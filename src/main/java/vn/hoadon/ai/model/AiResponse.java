package vn.hoadon.ai.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record AiResponse(
        String requestId,
        String text,
        JsonNode structuredOutput,
        List<AiFunctionCall> functionCalls,
        AiUsage usage,
        String provider,
        String model,
        String finishReason
) {
    public AiResponse {
        functionCalls = functionCalls == null ? List.of() : List.copyOf(functionCalls);
        usage = usage == null ? AiUsage.empty() : usage;
    }
}
