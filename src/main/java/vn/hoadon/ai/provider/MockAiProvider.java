package vn.hoadon.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
        JsonNode structured = request.responseJsonSchema() == null ? null : mockValue(request.responseJsonSchema());
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

    private JsonNode mockValue(JsonNode schema) {
        JsonNode enumValues = schema.path("enum");
        if (enumValues.isArray() && !enumValues.isEmpty()) return enumValues.get(0).deepCopy();
        String type = schema.path("type").asText();
        if ("object".equals(type)) {
            ObjectNode result = objectMapper.createObjectNode();
            JsonNode required = schema.path("required");
            JsonNode properties = schema.path("properties");
            if (required.isArray() && properties.isObject()) {
                for (JsonNode field : required) {
                    String name = field.asText();
                    result.set(name, mockValue(properties.path(name)));
                }
            }
            return result;
        }
        if ("array".equals(type)) {
            ArrayNode result = objectMapper.createArrayNode();
            result.add(mockValue(schema.path("items")));
            return result;
        }
        if ("number".equals(type) || "integer".equals(type)) {
            return objectMapper.getNodeFactory().numberNode(1);
        }
        if ("boolean".equals(type)) return objectMapper.getNodeFactory().booleanNode(false);
        return objectMapper.getNodeFactory().textNode("mock");
    }
}
