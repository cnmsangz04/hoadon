package vn.hoadon.invoice.template.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import vn.hoadon.invoice.template.service.TemplateException;

import java.util.*;

public record InvoiceCatalogSnapshot(
        String code, List<Group> groups, List<Dataset> datasets, List<Field> fields) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Group(
            Long id,
            String code,
            String label,
            String cardinality,
            String rootPath,
            int sortOrder,
            boolean active,
            Long lockVersion) {
        public Group(Long id, String code, String label, int sortOrder, boolean active, Long lockVersion) {
            this(id, code, label, "ONE", "/HDon", sortOrder, active, lockVersion);
        }
    }

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
            Long lockVersion,
            String applicableInvoiceTypes) {
        public Field(
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
            this(id, key, label, group, dataset, repeated, sourceType, sourcePath, valueType,
                    maxLength, defaultFormatter, description, requiredDisplay, active, sortOrder,
                    lockVersion, "1,2,6");
        }

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
                            lockVersion,
                            applicableInvoiceTypes);
        }

        public boolean supportsInvoiceType(int category, Integer stockIssueType) {
            if (applicableInvoiceTypes == null || applicableInvoiceTypes.isBlank()) return true;
            String categoryCode = String.valueOf(category);
            String stockIssueCode = stockIssueType == null ? null : categoryCode + ":" + stockIssueType;
            return Arrays.stream(applicableInvoiceTypes.split(","))
                    .map(String::trim)
                    .anyMatch(value -> value.equals(categoryCode)
                            || stockIssueCode != null && value.equals(stockIssueCode));
        }
    }

    public InvoiceCatalogSnapshot forInvoiceType(int category, Integer stockIssueType) {
        return new InvoiceCatalogSnapshot(
                code,
                groups,
                datasets,
                fields.stream()
                        .filter(field -> field.supportsInvoiceType(category, stockIssueType))
                        .toList());
    }

    public String rowScope(String datasetCode) {
        var dataset = requireDataset(datasetCode);
        if (dataset.parent() != null) {
            Set<String> seen = new HashSet<>();
            var current = dataset;
            while (true) {
                if (!seen.add(current.code()) || seen.size() > 16)
                    throw TemplateException.invalid("INVALID_CATALOG", "Danh sách cha có vòng hoặc quá sâu");
                if ("MANY".equals(current.cardinality())) return current.code();
                if (current.parent() == null) return current.code();
                current = requireDataset(current.parent());
            }
        }
        return "MANY".equals(dataset.cardinality()) ? dataset.code() : "document";
    }

    public String groupRowScope(String groupCode) {
        var group = requireGroup(groupCode);
        return "MANY".equals(group.cardinality()) ? group.code() : "document";
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

    public boolean isTopLevelList(String code) {
        var dataset = requireDataset(code);
        if (dataset.parent() != null)
            return "MANY".equals(dataset.cardinality())
                    && "document".equals(rowScope(dataset.parent()));
        return "MANY".equals(dataset.cardinality()) && !"document".equals(code);
    }

    public boolean isFieldAvailable(Field field) {
        if (!field.active()
                || groups.stream().noneMatch(g -> g.code().equals(field.group()) && g.active()))
            return false;
        return isDatasetActive(field.dataset());
    }

    public boolean isDatasetActive(String code) {
        var dataset = requireDataset(code);
        if (dataset.parent() != null) {
            Set<String> seen = new HashSet<>();
            while (true) {
                if (!seen.add(dataset.code()) || seen.size() > 16)
                    throw TemplateException.invalid("INVALID_CATALOG", "Danh sách cha có vòng hoặc quá sâu");
                if (!dataset.active()) return false;
                if (dataset.parent() == null) return true;
                dataset = requireDataset(dataset.parent());
            }
        }
        return dataset.active();
    }

    public String datasetXmlPath(String code) {
        var dataset = requireDataset(code);
        if (dataset.parent() != null) {
            List<String> paths = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            while (true) {
                if (!seen.add(dataset.code()) || seen.size() > 16)
                    throw TemplateException.invalid("INVALID_CATALOG", "Danh sách cha có vòng hoặc quá sâu");
                paths.addFirst(dataset.rootPath());
                if (dataset.parent() == null) return String.join("/", paths);
                dataset = requireDataset(dataset.parent());
            }
        }
        return dataset.rootPath().startsWith("/") ? dataset.rootPath() : "/HDon/" + dataset.rootPath();
    }

    public List<String> tableDatasets() {
        return datasets.stream()
                .filter(d -> isTopLevelList(d.code()))
                .filter(d -> fields.stream().anyMatch(f -> rowScope(f.dataset()).equals(d.code()) && isFieldAvailable(f)))
                .map(Dataset::code)
                .toList();
    }

    /**
     * A layout table is backed by one display group. The group's XML
     * cardinality only controls whether the table receives one row or many;
     * it does not decide whether the group can be used as a table.
     */
    public boolean isTableGroup(String code) {
        var group = requireGroup(code);
        var dataset = datasets.stream().filter(item -> code.equals(item.code())).findFirst().orElse(null);
        boolean nestedDataset = dataset != null && dataset.parent() != null
                && !"document".equals(rowScope(dataset.parent()));
        return !"document".equals(code)
                && group.active()
                && !nestedDataset
                && fields.stream().anyMatch(f ->
                        (code.equals(f.group()) || code.equals(rowScope(f.dataset())))
                                && "XML".equals(f.sourceType())
                                && isFieldAvailable(f));
    }

    public List<String> tableGroups() {
        return groups.stream()
                .filter(group -> isTableGroup(group.code()))
                .map(Group::code)
                .toList();
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

    public Group requireGroup(String code) {
        return groups.stream()
                .filter(g -> g.code().equals(code))
                .findFirst()
                .orElseThrow(
                        () ->
                                TemplateException.invalid(
                                        "UNKNOWN_GROUP", "Nhóm không tồn tại: " + code));
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
