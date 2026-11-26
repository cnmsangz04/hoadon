package vn.hoadon.ai.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record AiRequest(
        String systemInstruction,
        String prompt,
        JsonNode responseJsonSchema,
        List<AiFunctionDefinition> functions,
        Integer maxOutputTokens,
        Double temperature
) {
    public AiRequest {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("Nội dung yêu cầu AI không được để trống");
        }
        if (responseJsonSchema != null && !responseJsonSchema.isObject()) {
            throw new IllegalArgumentException("JSON Schema phản hồi AI không hợp lệ");
        }
        functions = functions == null ? List.of() : List.copyOf(functions);
        if (responseJsonSchema != null && !functions.isEmpty()) {
            throw new IllegalArgumentException("Không dùng đồng thời Structured Output và Function Calling");
        }
        if (maxOutputTokens != null && maxOutputTokens < 1) {
            throw new IllegalArgumentException("Số token phản hồi AI phải lớn hơn 0");
        }
        if (temperature != null && (temperature < 0 || temperature > 2)) {
            throw new IllegalArgumentException("Temperature AI phải nằm trong khoảng 0 đến 2");
        }
    }

    public static AiRequest text(String systemInstruction, String prompt) {
        return new AiRequest(systemInstruction, prompt, null, List.of(), null, null);
    }
}
