package vn.hoadon.entity.converters;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.List;

@Converter
public class LongListJsonConverter implements AttributeConverter<List<Long>, String> {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final TypeReference<List<Long>> TYPE = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<Long> attribute) {
        try {
            return JSON.writeValueAsString(attribute != null ? attribute : List.of());
        } catch (Exception e) {
            return "[]";
        }
    }

    @Override
    public List<Long> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) return new ArrayList<>();
            return JSON.readValue(dbData, TYPE);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
