package vn.hoadon.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import vn.hoadon.ai.model.AiFunctionCall;
import vn.hoadon.ai.model.AiUsage;

import java.util.List;

@Component
public class MockAiProvider implements AiProvider {
    private final ObjectMapper objectMapper;

    public MockAiProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String name() {
        return "mock";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public AiProviderResponse generate(AiProviderRequest request) {
        JsonNode structured = request.responseJsonSchema() == null ? null : objectMapper.createObjectNode();
        List<AiFunctionCall> calls = request.functions().isEmpty()
                ? List.of()
                : List.of(new AiFunctionCall(request.functions().get(0).name(), objectMapper.createObjectNode()));
        String text = structured == null && calls.isEmpty() ? "Mock AI response" : "";
        long inputTokens = Math.max(1, request.prompt().length() / 4L);
        long outputTokens = text.isEmpty() ? 1 : Math.max(1, text.length() / 4L);
        return new AiProviderResponse(text, structured, calls,
                new AiUsage(inputTokens, outputTokens, inputTokens + outputTokens),
                "mock", "STOP");
    }
}
