package vn.hoadon.invoice.template.catalog;

import vn.hoadon.invoice.template.service.TemplateException;

import java.util.*;

public record InvoiceCatalogSnapshot(
        String code, List<Group> groups, List<Dataset> datasets, List<Field> fields) {
    public record Group(
            Long id, String code, String label, int sortOrder, boolean active, Long lockVersion) {}

    public record Dataset(
            Long id,
            String code,
            String label,
            String parent,
            String cardinality,
            String rootPath,
            boolean active,
            Long lockVersion) {}

    public record Field(
            Long id,
            String key,
            String label,
            String group,
            String dataset,
            boolean repeated,
            String sourceType,
            String sourcePath,
            String valueType,
            Integer maxLength,
            String defaultFormatter,
            String description,
            boolean requiredDisplay,
            boolean active,
            int sortOrder,
            Long lockVersion) {
        public Field withRepeated(boolean value) {
            return repeated == value
                    ? this
                    : new Field(
                            id,
                            key,
                            label,
                            group,
                            dataset,
                            value,
                            sourceType,
                            sourcePath,
                            valueType,
                            maxLength,
                            defaultFormatter,
                            description,
                            requiredDisplay,
                            active,
                            sortOrder,
                            lockVersion);
        }
    }

    public String rowScope(String datasetCode) {
        Set<String> seen = new HashSet<>();
        var current = requireDataset(datasetCode);
        while (true) {
            if (!seen.add(current.code()) || seen.size() > 16)
                throw TemplateException.invalid(
                        "INVALID_CATALOG", "Danh sách cha có vòng hoặc quá sâu");
            if ("MANY".equals(current.cardinality())) return current.code();
            if (current.parent() == null) return current.code();
            current = requireDataset(current.parent());
        }
    }

    public InvoiceCatalogSnapshot withDerivedScopes() {
        return new InvoiceCatalogSnapshot(
                code,
                groups,
                datasets,
                fields.stream()
                        .map(
                                field ->
                                        field.withRepeated(
                                                !"document".equals(rowScope(field.dataset()))))
                        .toList());
    }

    public Dataset requireDataset(String code) {
        return datasets.stream()
                .filter(d -> d.code().equals(code))
                .findFirst()
                .orElseThrow(
                        () ->
                                TemplateException.invalid(
                                        "UNKNOWN_DATASET",
                                        "Danh sách dữ liệu không tồn tại: " + code));
    }

    public Field requireField(String key) {
        return fields.stream()
                .filter(f -> f.key().equals(key))
                .findFirst()
                .orElseThrow(
                        () ->
                                TemplateException.invalid(
                                        "UNKNOWN_FIELD", "Phần tử không tồn tại: " + key));
    }
}
