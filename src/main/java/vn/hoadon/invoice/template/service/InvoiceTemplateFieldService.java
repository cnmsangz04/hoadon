package vn.hoadon.invoice.template.service;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

import vn.hoadon.invoice.template.catalog.*;
import vn.hoadon.invoice.template.persistence.*;

import java.time.*;
import java.util.*;

@Service
@Transactional(isolation = Isolation.SERIALIZABLE)
public class InvoiceTemplateFieldService {
    public record FieldInput(
            String key,
            String label,
            String group,
            String dataset,
            String sourceType,
            String sourcePath,
            String valueType,
            Integer maxLength,
            String defaultFormatter,
            String description,
            boolean requiredDisplay,
            boolean active,
            Integer sortOrder,
            Long expectedLockVersion) {}

    public record GroupInput(
            String code, String label, int sortOrder, boolean active, Long expectedLockVersion) {}

    public record DatasetInput(
            String code,
            String label,
            String parent,
            String cardinality,
            String rootPath,
            boolean active,
            Long expectedLockVersion) {}

    public record FieldList(
            List<InvoiceCatalogSnapshot.Field> items, long total, int page, int size) {}

    private final InvoiceTemplateFieldRepository fields;
    private final InvoiceTemplateFieldGroupRepository groups;
    private final InvoiceTemplateDatasetRepository datasets;
    private final InvoiceFieldCatalog catalog;
    private final InvoiceExternalFieldRegistry context;

    public InvoiceTemplateFieldService(
            InvoiceTemplateFieldRepository fields,
            InvoiceTemplateFieldGroupRepository groups,
            InvoiceTemplateDatasetRepository datasets,
            InvoiceFieldCatalog catalog,
            InvoiceExternalFieldRegistry context) {
        this.fields = fields;
        this.groups = groups;
        this.datasets = datasets;
        this.catalog = catalog;
        this.context = context;
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
        return new FieldList(
                result.stream().map(this::row).toList(),
                result.getTotalElements(),
                pageable.getPageNumber() + 1,
                pageable.getPageSize());
    }

    @Transactional(readOnly = true)
    public Object metadata(TemplateAccess access) {
        access.requireSystemManager();
        return Map.of(
                "groups",
                groups.findAllByOrderBySortOrderAscIdAsc(),
                "datasets",
                datasets.findAll(Sort.by("id")),
                "contextFields",
                context.available(),
                "nextSortOrder",
                fields.maximumSortOrder() + 10);
    }

    public InvoiceCatalogSnapshot.Field create(FieldInput input, TemplateAccess access) {
        access.requireSystemManager();
        if (input.key() == null
                || !input.key().matches("[a-z][A-Za-z0-9]*(?:\\.[A-Za-z][A-Za-z0-9]*)+")
                || input.key().length() > 160) invalid("Mã phần tử không hợp lệ");
        if (fields.existsByFieldKey(input.key()))
            throw TemplateException.conflict("Mã phần tử đã tồn tại");
        var entity = new InvoiceTemplateFieldEntity();
        entity.setFieldKey(input.key());
        entity.setCreatedAt(now());
        entity.setCreatedBy(access.actorId());
        entity = apply(entity, input, access);
        catalog.checkpoint(access.actorId());
        return row(entity);
    }

    public InvoiceCatalogSnapshot.Field update(Long id, FieldInput input, TemplateAccess access) {
        access.requireSystemManager();
        var entity = fields.lockById(id).orElseThrow(TemplateException::missing);
        revision(entity.getLockVersion(), input.expectedLockVersion());
        if (!entity.getFieldKey().equals(input.key())) invalid("Mã phần tử không được thay đổi");
        entity = apply(entity, input, access);
        catalog.checkpoint(access.actorId());
        return row(entity);
    }

    private InvoiceTemplateFieldEntity apply(
            InvoiceTemplateFieldEntity entity, FieldInput input, TemplateAccess access) {
        if (input.label() == null
                || input.label().isBlank()
                || input.label().length() > 500
                || input.group() == null
                || input.dataset() == null) invalid("Thiếu tên, nhóm hoặc phạm vi dữ liệu");
        var group = groups.findByCode(input.group()).orElseThrow(TemplateException::missing);
        var dataset = datasets.findByCode(input.dataset()).orElseThrow(TemplateException::missing);
        if (!group.isActive() || !dataset.isActive())
            invalid("Nhóm hoặc phạm vi dữ liệu đã ngừng sử dụng");
        if (input.sourcePath() == null || input.sourcePath().length() > 700)
            invalid("Đường dẫn nguồn không hợp lệ");
        if (!Set.of("XML", "CONTEXT").contains(Objects.toString(input.sourceType(), "")))
            invalid("Nguồn dữ liệu không hợp lệ");
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
        entity.setDatasetId(dataset.getId());
        entity.setSourceType(input.sourceType());
        entity.setSourcePath(input.sourcePath().trim());
        entity.setValueType(input.valueType());
        entity.setMaxLength(input.maxLength());
        entity.setDefaultFormatter(input.defaultFormatter());
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
        entity.setCode(input.code());
        entity.setLabel(input.label().trim());
        entity.setSortOrder(input.sortOrder());
        entity.setActive(input.active());
        entity = groups.saveAndFlush(entity);
        catalog.checkpoint(access.actorId());
        return entity;
    }

    public Object saveDataset(Long id, DatasetInput input, TemplateAccess access) {
        access.requireSystemManager();
        validateCode(input.code(), input.label());
        if (input.rootPath() == null
                || input.rootPath().length() > 700
                || !Set.of("ONE", "MANY").contains(Objects.toString(input.cardinality(), "")))
            invalid("Phạm vi dữ liệu không hợp lệ");
        var entity =
                id == null
                        ? new InvoiceTemplateDatasetEntity()
                        : datasets.findById(id).orElseThrow(TemplateException::missing);
        if (id != null) {
            revision(entity.getLockVersion(), input.expectedLockVersion());
            if (!entity.getCode().equals(input.code()))
                invalid("Mã phạm vi dữ liệu không được thay đổi");
        }
        if (id == null && datasets.findByCode(input.code()).isPresent())
            throw TemplateException.conflict("Mã phạm vi dữ liệu đã tồn tại");
        entity.setCode(input.code());
        entity.setLabel(input.label().trim());
        entity.setParentId(
                input.parent() == null || input.parent().isBlank()
                        ? null
                        : datasets.findByCode(input.parent())
                                .orElseThrow(TemplateException::missing)
                                .getId());
        entity.setCardinality(input.cardinality());
        entity.setRootPath(input.rootPath());
        entity.setActive(input.active());
        entity = datasets.saveAndFlush(entity);
        catalog.checkpoint(access.actorId());
        return entity;
    }

    private InvoiceCatalogSnapshot.Field row(InvoiceTemplateFieldEntity f) {
        var group = groups.findById(f.getGroupId()).orElseThrow();
        var dataset = datasets.findById(f.getDatasetId()).orElseThrow();
        var scope = dataset;
        Set<Long> seen = new HashSet<>();
        while (!"MANY".equals(scope.getCardinality()) && scope.getParentId() != null) {
            if (!seen.add(scope.getId()) || seen.size() > 16)
                invalid("Danh sách cha có vòng hoặc quá sâu");
            scope = datasets.findById(scope.getParentId()).orElseThrow();
        }
        return new InvoiceCatalogSnapshot.Field(
                f.getId(),
                f.getFieldKey(),
                f.getLabel(),
                group.getCode(),
                dataset.getCode(),
                "MANY".equals(scope.getCardinality()),
                f.getSourceType(),
                f.getSourcePath(),
                f.getValueType(),
                f.getMaxLength(),
                f.getDefaultFormatter(),
                f.getDescription(),
                f.isRequiredDisplay(),
                f.isActive(),
                f.getSortOrder(),
                f.getLockVersion());
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
