package vn.hoadon.ai.model;

import com.fasterxml.jackson.databind.JsonNode;

public record AiFunctionDefinition(String name, String description, JsonNode parametersJsonSchema) {
    public AiFunctionDefinition {
        if (name == null || !name.matches("[A-Za-z_][A-Za-z0-9_.-]{0,63}")) {
            throw new IllegalArgumentException("Tên function AI không hợp lệ");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Mô tả function AI không được để trống");
        }
        if (parametersJsonSchema == null || !parametersJsonSchema.isObject()) {
            throw new IllegalArgumentException("JSON Schema của function AI không hợp lệ");
        }
    }
}
