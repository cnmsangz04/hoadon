package vn.hoadon.ai.model;

import com.fasterxml.jackson.databind.JsonNode;

public record AiFunctionCall(String name, JsonNode arguments) {
}
