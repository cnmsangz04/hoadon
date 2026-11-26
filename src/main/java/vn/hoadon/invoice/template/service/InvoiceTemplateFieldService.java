package vn.hoadon.invoice.template.service;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import vn.hoadon.invoice.template.catalog.*;
import vn.hoadon.invoice.template.persistence.*;

import java.time.*;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class InvoiceTemplateFieldService {
    public record FieldInput(
            String key,
            String label,
            String group,
            String sourceType,
            String sourcePath,
            String valueType,
            Integer maxLength,
            String defaultFormatter,
            String applicableInvoiceTypes,
            String description,
            boolean requiredDisplay,
            boolean active,
            Integer sortOrder,
            Long expectedLockVersion) {}

    public record GroupInput(
            String code,
            String label,
            String cardinality,
            String rootPath,
            int sortOrder,
            boolean active,
            Long expectedLockVersion) {}

    public record FieldList(
            List<InvoiceCatalogSnapshot.Field> items, long total, int page, int size) {}

    private final InvoiceTemplateFieldRepository fields;
    private final InvoiceTemplateFieldGroupRepository groups;
    private final InvoiceFieldCatalog catalog;

    public InvoiceTemplateFieldService(
            InvoiceTemplateFieldRepository fields,
            InvoiceTemplateFieldGroupRepository groups,
            InvoiceFieldCatalog catalog) {
        this.fields = fields;
        this.groups = groups;
        this.catalog = catalog;
    }

    @Transactional(readOnly = true)
    public FieldList list(
            String query, String group, Boolean active, int page, int size, TemplateAccess access) {
        access.requireSystemManager();
        var pageable =
                PageRequest.of(
                        Math.max(0, page - 1),
                        Math.clamp(size, 1, 100),
                        Sort.by("sortOrder", "id"));
        Long groupId =
                group == null || group.isBlank()
                        ? null
                        : groups.findByCode(group)
                                .map(InvoiceTemplateFieldGroupEntity::getId)
                                .orElse(-1L);
        var result =
                fields.findAll(
                        (root, cq, cb) -> {
                            List<Predicate> filters = new ArrayList<>();
                            if (groupId != null)
                                filters.add(cb.equal(root.get("groupId"), groupId));
                            if (active != null) filters.add(cb.equal(root.get("active"), active));
                            if (query != null && !query.isBlank()) {
                                String keyword =
                                        "%"
                                                + query.trim()
                                                        .toLowerCase(Locale.ROOT)
                                                        .replace("\\", "\\\\")
                                                        .replace("%", "\\%")
                                                        .replace("_", "\\_")
                                                + "%";
                                filters.add(
                                        cb.or(
                                                cb.like(
                                                        cb.lower(root.get("fieldKey")),
                                                        keyword,
                                                        '\\'),
                                                cb.like(cb.lower(root.get("label")), keyword, '\\'),
                                                cb.like(
                                                        cb.lower(root.get("sourcePath")),
                                                        keyword,
                                                        '\\')));
                            }
                            return cb.and(filters.toArray(Predicate[]::new));
                        },
                        pageable);
        var source = catalog.working();
        return new FieldList(
                result.stream().map(f -> source.requireField(f.getFieldKey())).toList(),
                result.getTotalElements(),
                pageable.getPageNumber() + 1,
                pageable.getPageSize());
    }

    @Transactional(readOnly = true)
    public Object metadata(TemplateAccess access) {
        access.requireSystemManager();
        var source = catalog.working();
        var groupMetadata = source.groups().stream().map(group -> {
            var groupFields = source.fields().stream().filter(f -> f.group().equals(group.code())).toList();
            var item = new LinkedHashMap<String, Object>();
            item.put("id", group.id());
            item.put("code", group.code());
            item.put("label", group.label());
            item.put("sortOrder", group.sortOrder());
            item.put("active", group.active());
            item.put("lockVersion", group.lockVersion());
            item.put("cardinality", group.cardinality());
            item.put("rootPath", group.rootPath());
            item.put("fieldCount", groupFields.size());
            return item;
        }).toList();
        return Map.of(
                "groups", groupMetadata,
                "groupUsage",
                source.groups().stream().collect(java.util.stream.Collectors.toMap(
                        InvoiceCatalogSnapshot.Group::code,
                        group -> Map.of(
                                "fieldCount", source.fields().stream().filter(f -> f.group().equals(group.code())).count(),
                                "xmlRoot", group.rootPath(),
                                "active", group.active()
                        ))),
                "nextSortOrder",
                fields.maximumSortOrder() + 10);
    }

    public InvoiceCatalogSnapshot.Field create(FieldInput input, TemplateAccess access) {
        access.requireSystemManager();
        String key = generatedFieldKey(input.group(), input.sourcePath());
        if (!key.matches("[a-z][A-Za-z0-9]*(?:\\.[A-Za-z][A-Za-z0-9]*)+") || key.length() > 160)
            invalid("Mã phần tử không hợp lệ");
        if (fields.existsByFieldKey(key))
            throw TemplateException.conflict("Mã phần tử đã tồn tại");
        var entity = new InvoiceTemplateFieldEntity();
        entity.setFieldKey(key);
        entity.setCreatedAt(now());
        entity.setCreatedBy(access.actorId());
        entity = apply(entity, input, access);
        catalog.checkpoint(access.actorId());
        return catalog.working().requireField(entity.getFieldKey());
    }

    private String generatedFieldKey(String group, String sourcePath) {
        String prefix = identifier(group, "field");
        if (sourcePath == null || sourcePath.isBlank()) invalid("Thiếu đường dẫn XML tương đối");
        String path = Arrays.stream(sourcePath.trim().split("/"))
                .map(this::pathSegmentIdentifier)
                .filter(segment -> !segment.isBlank())
                .reduce((left, right) -> left + "." + right)
                .orElseThrow(() -> TemplateException.invalid("INVALID_FIELD_KEY", "Đường dẫn XML tương đối không hợp lệ"));
        return prefix + "." + path;
    }

    private String pathSegmentIdentifier(String segment) {
        String normalized = normalize(segment).trim();
        Matcher base = Pattern.compile("^([^\\[]+)").matcher(normalized);
        if (!base.find()) return "";
        StringBuilder result = new StringBuilder(xmlIdentifier(base.group(1)));
        Matcher predicate = Pattern.compile("\\[[^=\\]]+=(?:'([^']*)'|\"([^\"]*)\")\\]").matcher(normalized);
        while (predicate.find()) {
            String value = predicate.group(1) == null ? predicate.group(2) : predicate.group(1);
            result.append(xmlIdentifier(value));
        }
        return result.toString();
    }

    private String xmlIdentifier(String value) {
        String result = value == null ? "" : value.replaceAll("[^A-Za-z0-9]+", "");
        if (result.isBlank()) return "";
        return Character.isLetter(result.charAt(0)) ? result : "Field" + result;
    }

    private String identifier(String value, String fallback) {
        String normalized = normalize(value);
        StringJoiner result = new StringJoiner(".");
        for (String segment : normalized.replaceAll("[^A-Za-z0-9.]+", " ").split("\\.")) {
            segment = segment.replaceAll("\\s+", "");
            if (segment.isBlank() || !Character.isLetter(segment.charAt(0))) return fallback;
            result.add(Character.toLowerCase(segment.charAt(0)) + segment.substring(1));
        }
        return result.length() == 0 ? fallback : result.toString();
    }

    private String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    public InvoiceCatalogSnapshot.Field update(Long id, FieldInput input, TemplateAccess access) {
        access.requireSystemManager();
        var entity = fields.lockById(id).orElseThrow(TemplateException::missing);
        revision(entity.getLockVersion(), input.expectedLockVersion());
        if (!entity.getFieldKey().equals(input.key())) invalid("Mã phần tử không được thay đổi");
        var currentGroup = groups.findById(entity.getGroupId()).orElseThrow(TemplateException::missing);
        if (!Objects.equals(entity.getLabel(), Objects.toString(input.label(), "").trim())
                || !Objects.equals(currentGroup.getCode(), Objects.toString(input.group(), "").trim())
                || !Objects.equals(entity.getSourceType(), Objects.toString(input.sourceType(), "").trim())
                || !Objects.equals(entity.getSourcePath(), Objects.toString(input.sourcePath(), "").trim())) {
            invalid("Không thể thay đổi tên, nhóm hoặc đường dẫn của phần tử đã tạo");
        }
        entity = apply(entity, input, access);
        catalog.checkpoint(access.actorId());
        return catalog.working().requireField(entity.getFieldKey());
    }

    public void deleteField(Long id, Long expectedLockVersion, TemplateAccess access) {
        access.requireSystemManager();
        var entity = fields.lockById(id).orElseThrow(TemplateException::missing);
        revision(entity.getLockVersion(), expectedLockVersion);
        fields.delete(entity);
        fields.flush();
        catalog.checkpoint(access.actorId());
    }

    private InvoiceTemplateFieldEntity apply(
            InvoiceTemplateFieldEntity entity, FieldInput input, TemplateAccess access) {
        if (input.label() == null
                || input.label().isBlank()
                || input.label().length() > 500
                || input.group() == null) invalid("Thiếu tên hoặc nhóm");
        var group = groups.findByCode(input.group()).orElseThrow(TemplateException::missing);
        var working = catalog.working();
        if (!group.isActive())
            invalid("Nhóm đã ngừng sử dụng");
        if (input.sourcePath() == null || input.sourcePath().length() > 700)
            invalid("Đường dẫn nguồn không hợp lệ");
        String sourcePath = input.sourcePath().trim();
        if (sourcePath.startsWith("/") || sourcePath.startsWith("HDon/"))
            invalid("Đường dẫn XML phải là đường dẫn tương đối của bảng dữ liệu");
        if (!"XML".equals(input.sourceType())) invalid("Phần tử chỉ được lấy dữ liệu từ XML hóa đơn");
        if (!Set.of("STRING", "INTEGER", "DECIMAL", "DATE", "BOOLEAN")
                .contains(Objects.toString(input.valueType(), "")))
            invalid("Kiểu dữ liệu không hợp lệ");
        if (input.maxLength() != null && input.maxLength() < 1) invalid("Độ dài phải lớn hơn 0");
        if (input.description() != null && input.description().length() > 2000)
            invalid("Mô tả vượt 2000 ký tự");
        if (input.defaultFormatter() == null
                || !Set.of("plainText", "date", "decimal", "moneyByCurrency", "invoiceNumber")
                        .contains(input.defaultFormatter())) invalid("Định dạng không hợp lệ");
        entity.setLabel(input.label().trim());
        entity.setGroupId(group.getId());
        entity.setSourceType(input.sourceType());
        entity.setSourcePath(input.sourcePath().trim());
        entity.setValueType(input.valueType());
        entity.setMaxLength(input.maxLength());
        entity.setDefaultFormatter(input.defaultFormatter());
        entity.setApplicableInvoiceTypes(normalizeInvoiceTypes(input.applicableInvoiceTypes()));
        entity.setDescription(input.description());
        entity.setRequiredDisplay(input.requiredDisplay());
        entity.setActive(input.active());
        entity.setSortOrder(
                input.sortOrder() == null ? fields.maximumSortOrder() + 10 : input.sortOrder());
        if (entity.getSortOrder() < 0 || entity.getSortOrder() > 1000000)
            invalid("Thứ tự không hợp lệ");
        entity.setUpdatedBy(access.actorId());
        entity.setUpdatedAt(now());
        return fields.saveAndFlush(entity);
    }

    private String normalizeInvoiceTypes(String value) {
        if (value == null || value.isBlank()) invalid("Phải chọn ít nhất một loại hóa đơn");
        var allowed = List.of("1", "2", "6", "6:1", "6:2");
        var selected = Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
        if (selected.isEmpty() || selected.stream().anyMatch(item -> !allowed.contains(item)))
            invalid("Loại hóa đơn áp dụng không hợp lệ");
        return allowed.stream().filter(selected::contains).collect(java.util.stream.Collectors.joining(","));
    }

    public Object saveGroup(Long id, GroupInput input, TemplateAccess access) {
        access.requireSystemManager();
        validateCode(input.code(), input.label());
        var entity =
                id == null
                        ? new InvoiceTemplateFieldGroupEntity()
                        : groups.findById(id).orElseThrow(TemplateException::missing);
        if (id != null) {
            revision(entity.getLockVersion(), input.expectedLockVersion());
            if (!entity.getCode().equals(input.code())) invalid("Mã nhóm không được thay đổi");
        }
        if (id == null && groups.findByCode(input.code()).isPresent())
            throw TemplateException.conflict("Mã nhóm đã tồn tại");
        if (input.cardinality() == null || !Set.of("ONE", "MANY").contains(input.cardinality()))
            invalid("Kiểu nhóm không hợp lệ");
        if (input.rootPath() == null || input.rootPath().isBlank() || input.rootPath().length() > 700)
            invalid("Đường dẫn XML của nhóm không hợp lệ");
        if (!input.rootPath().trim().matches("/HDon(?:/[A-Za-z_][A-Za-z0-9_.-]*)*"))
            invalid("Đường dẫn XML của bảng phải là đường dẫn tuyệt đối bắt đầu bằng /HDon");
        entity.setCode(input.code());
        entity.setLabel(input.label().trim());
        entity.setCardinality(input.cardinality());
        entity.setRootPath(input.rootPath().trim());
        entity.setSortOrder(input.sortOrder());
        entity.setActive(input.active());
        entity = groups.saveAndFlush(entity);
        catalog.checkpoint(access.actorId());
        return entity;
    }

    public void deleteGroup(Long id, Long expectedLockVersion, TemplateAccess access) {
        access.requireSystemManager();
        var entity = groups.findById(id).orElseThrow(TemplateException::missing);
        revision(entity.getLockVersion(), expectedLockVersion);
        if (fields.countByGroupId(id) > 0)
            throw TemplateException.conflict("Không thể xóa nhóm đang được sử dụng");
        groups.delete(entity);
        groups.flush();
        catalog.checkpoint(access.actorId());
    }

    private static LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private static void invalid(String message) {
        throw TemplateException.invalid("INVALID_TEMPLATE_FIELD", message);
    }

    private static void revision(Long actual, Long expected) {
        if (!Objects.equals(actual, expected))
            throw TemplateException.conflict("Dữ liệu đã thay đổi; vui lòng tải lại");
    }

    private static void validateCode(String code, String label) {
        if (code == null
                || !code.matches("[a-z][A-Za-z0-9]*(?:\\.[A-Za-z][A-Za-z0-9]*)*")
                || code.length() > 80
                || label == null
                || label.isBlank()
                || label.length() > 255) invalid("Mã hoặc tên không hợp lệ");
    }
}
