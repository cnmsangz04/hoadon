package vn.hoadon.invoice.template.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import vn.hoadon.invoice.template.persistence.*;
import vn.hoadon.invoice.template.service.TemplateException;
import vn.hoadon.invoice.template.storage.DocumentStorage;

import java.time.*;
import java.util.*;

@Component
public class InvoiceFieldCatalog {
    private final InvoiceTemplateFieldRepository fields;
    private final InvoiceTemplateFieldGroupRepository groups;
    private final InvoiceTemplateCatalogReleaseRepository releases;
    private final ObjectMapper mapper;
    private final InvoiceCatalogSnapshot fixed;

    @Autowired
    public InvoiceFieldCatalog(
            InvoiceTemplateFieldRepository fields,
            InvoiceTemplateFieldGroupRepository groups,
            InvoiceTemplateCatalogReleaseRepository releases,
            ObjectMapper mapper) {
        this.fields = fields;
        this.groups = groups;
        this.releases = releases;
        this.mapper = mapper;
        this.fixed = null;
    }

    public InvoiceFieldCatalog(InvoiceCatalogSnapshot fixed) {
        this.fixed = fixed.withDerivedScopes();
        this.fields = null;
        this.groups = null;
        this.releases = null;
        this.mapper = null;
    }

    public String version() {
        return latest().code();
    }

    public InvoiceCatalogSnapshot latest() {
        if (fixed != null) return fixed;
        return snapshot(
                releases.findTopByOrderByIdDesc()
                        .orElseThrow(
                                () ->
                                        TemplateException.invalid(
                                                "CATALOG_MISSING", "Danh mục chưa được khởi tạo"))
                        .getCode());
    }

    public InvoiceCatalogSnapshot snapshot(String code) {
        if ("CURRENT".equalsIgnoreCase(code)) return latest();
        if (fixed != null && fixed.code().equals(code)) return fixed;
        if (fixed != null)
            throw TemplateException.invalid("CATALOG_MISSING", "Danh mục không tồn tại");
        var release =
                releases.findByCode(code)
                        .orElseThrow(
                                () ->
                                        TemplateException.invalid(
                                                "CATALOG_MISSING",
                                                "Không tìm thấy danh mục: " + code));
        if (!DocumentStorage.sha256(release.getSnapshotJson()).equals(release.getContentHash()))
            throw new IllegalStateException("Catalog checksum mismatch");
        try {
            return ensureCompatibilityFields(
                    mapper.readValue(release.getSnapshotJson(), InvoiceCatalogSnapshot.class));
        } catch (Exception ex) {
            throw new IllegalStateException("Không đọc được danh mục", ex);
        }
    }

    /**
     * Existing layouts can keep an older catalog version. Keep the two XML
     * document fields available in those snapshots after the old CONTEXT
     * fields were removed from the working catalog.
     */
    private InvoiceCatalogSnapshot ensureCompatibilityFields(InvoiceCatalogSnapshot source) {
        List<InvoiceCatalogSnapshot.Group> groups = new ArrayList<>(source.groups());
        if (groups.stream().noneMatch(group -> "invoice".equals(group.code()))) {
            groups.add(new InvoiceCatalogSnapshot.Group(
                    null, "invoice", "Thông tin hóa đơn", "ONE", "/HDon", 1000, true, 0L));
        }

        List<InvoiceCatalogSnapshot.Dataset> datasets = new ArrayList<>(source.datasets());
        if (datasets.stream().noneMatch(item -> "document".equals(item.code()))) {
            datasets.add(new InvoiceCatalogSnapshot.Dataset(
                    null, "document", "Hóa đơn", null, "ONE", "/HDon", true, 0L));
        }
        String dataset = "document";
        List<InvoiceCatalogSnapshot.Field> fields = new ArrayList<>();
        Set<String> compatibilityKeys = Set.of("invoice.MCCQT", "invoice.DSCKS.NBan.Signature.SignedAt", "invoice.other.TTinMTCuu.DLieu");
        for (var field : source.fields()) {
            if ("invoice.signature.SignedAt".equals(field.key())) continue;
            boolean currentLookupField = "invoice.other.TTinMTCuu.DLieu".equals(field.key())
                    && "invoice.other".equals(field.group())
                    && "TTin[TTruong='MTCuu']/DLieu".equals(field.sourcePath());
            boolean currentSignatureField = "invoice.DSCKS.NBan.Signature.SignedAt".equals(field.key())
                    && "invoice".equals(field.group())
                    && "DSCKS/NBan/Signature/SignedAt".equals(field.sourcePath());
            if (!compatibilityKeys.contains(field.key()) || currentLookupField || currentSignatureField) {
                fields.add(field);
                continue;
            }
            fields.add(new InvoiceCatalogSnapshot.Field(
                    field.id(), field.key(), field.label(), "invoice", dataset, false,
                    "XML", compatibilityPath(field.key()), "STRING", 255,
                    "plainText", field.description(), field.requiredDisplay(), field.active(),
                    field.sortOrder(), field.lockVersion()));
        }
        if (fields.stream().noneMatch(field -> "invoice.MCCQT".equals(field.key()))) {
            fields.add(compatibilityField("invoice.MCCQT", "Mã của cơ quan thuế", "MCCQT", dataset, 1000));
        }
        if (fields.stream().noneMatch(field -> "invoice.DSCKS.NBan.Signature.SignedAt".equals(field.key()))) {
            fields.add(compatibilityField(
                    "invoice.DSCKS.NBan.Signature.SignedAt", "Ngày ký số",
                    "DSCKS/NBan/Signature/SignedAt", dataset, 1005));
        }
        if (fields.stream().noneMatch(field -> "invoice.other.TTinMTCuu.DLieu".equals(field.key()))) {
            fields.add(compatibilityField(
                    "invoice.other.TTinMTCuu.DLieu", "Mã tra cứu hóa đơn",
                    "DLHDon/TTKhac/TTin[TTruong='MTCuu']/DLieu", dataset, 1010));
        }
        return new InvoiceCatalogSnapshot(source.code(), groups, datasets, fields).withDerivedScopes();
    }

    private InvoiceCatalogSnapshot.Field compatibilityField(
            String key, String label, String path, String dataset, int sortOrder) {
        String description = "invoice.DSCKS.NBan.Signature.SignedAt".equals(key)
                ? "Ngày ký số từ HDon/DSCKS/NBan/Signature, đã được Java định dạng"
                : "Phần tử XML tương thích với mẫu cũ";
        return new InvoiceCatalogSnapshot.Field(
                null, key, label, "invoice", dataset, false, "XML", path, "STRING", 255,
                "plainText", description, false, true, sortOrder, 0L);
    }

    private String compatibilityPath(String key) {
        return switch (key) {
            case "invoice.MCCQT" -> "MCCQT";
            case "invoice.DSCKS.NBan.Signature.SignedAt" -> "DSCKS/NBan/Signature/SignedAt";
            default -> "DLHDon/TTKhac/TTin[TTruong='MTCuu']/DLieu";
        };
    }

    public InvoiceCatalogSnapshot working() {
        var groupRows = groups.findAllByOrderBySortOrderAscIdAsc();
        Map<Long, String> groupCodes = new HashMap<>();
        groupRows.forEach(group -> groupCodes.put(group.getId(), group.getCode()));
        var groupDefinitions =
                groupRows.stream()
                        .map(
                                group ->
                                        new InvoiceCatalogSnapshot.Group(
                                                group.getId(),
                                                group.getCode(),
                                                group.getLabel(),
                                                group.getCardinality(),
                                                group.getRootPath(),
                                                group.getSortOrder(),
                                                group.isActive(),
                                                group.getLockVersion()))
                        .toList();
        List<InvoiceCatalogSnapshot.Dataset> datasetDefinitions = new ArrayList<>();
        datasetDefinitions.add(
                new InvoiceCatalogSnapshot.Dataset(
                        null, "document", "Hóa đơn", null, "ONE", "/HDon", true, 0L));
        groupDefinitions.stream()
                .filter(group -> !"document".equals(group.code()) && "MANY".equals(group.cardinality()))
                .map(
                        group ->
                                new InvoiceCatalogSnapshot.Dataset(
                                        group.id(),
                                        group.code(),
                                        group.label(),
                                        null,
                                        group.cardinality(),
                                        group.rootPath(),
                                        group.active(),
                                        group.lockVersion()))
                .forEach(datasetDefinitions::add);
        var structure =
                new InvoiceCatalogSnapshot(
                        "CURRENT", groupDefinitions, datasetDefinitions, List.of());
        List<InvoiceCatalogSnapshot.Field> definitions = new ArrayList<>();
        for (var field : fields.findAllByOrderBySortOrderAscIdAsc()) {
            String groupCode = groupCodes.get(field.getGroupId());
            if (groupCode == null)
                throw TemplateException.invalid("INVALID_CATALOG", "Nhóm chưa được gắn phạm vi dữ liệu");
            String dataset = structure.groupRowScope(groupCode);
            definitions.add(
                    new InvoiceCatalogSnapshot.Field(
                            field.getId(),
                            field.getFieldKey(),
                            field.getLabel(),
                            groupCode,
                            dataset,
                            !"document".equals(structure.rowScope(dataset)),
                            field.getSourceType(),
                            field.getSourcePath(),
                            field.getValueType(),
                            field.getMaxLength(),
                            field.getDefaultFormatter(),
                            field.getDescription(),
                            field.isRequiredDisplay(),
                            field.isActive(),
                            field.getSortOrder(),
                            field.getLockVersion()));
        }
        return new InvoiceCatalogSnapshot(
                structure.code(), groupDefinitions, datasetDefinitions, definitions);
    }

    @Transactional
    public void checkpoint(Long actor) {
        var value = working();
        InvoiceCatalogValidator.validate(value);
        var snapshot =
                new InvoiceCatalogSnapshot(
                        "catalog-" + UUID.randomUUID(),
                        value.groups(),
                        value.datasets(),
                        value.fields());
        try {
            var release = new InvoiceTemplateCatalogReleaseEntity();
            release.setCode(snapshot.code());
            release.setSnapshotJson(mapper.writeValueAsString(snapshot));
            release.setContentHash(DocumentStorage.sha256(release.getSnapshotJson()));
            release.setPublishedBy(actor);
            release.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
            releases.saveAndFlush(release);
        } catch (Exception ex) {
            throw new IllegalStateException("Không lưu được danh mục", ex);
        }
    }
}
