package vn.hoadon.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import vn.hoadon.ai.model.AiFunctionDefinition;

import java.util.List;

public record AiProviderRequest(
        String systemInstruction,
        String prompt,
        JsonNode responseJsonSchema,
        List<AiFunctionDefinition> functions,
        int maxOutputTokens,
        Double temperature
) {
    public AiProviderRequest {
        functions = functions == null ? List.of() : List.copyOf(functions);
    }
}
