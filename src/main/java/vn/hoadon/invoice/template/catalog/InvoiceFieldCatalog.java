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
    private final InvoiceTemplateDatasetRepository datasets;
    private final InvoiceTemplateCatalogReleaseRepository releases;
    private final ObjectMapper mapper;
    private final InvoiceCatalogSnapshot fixed;

    @Autowired
    public InvoiceFieldCatalog(
            InvoiceTemplateFieldRepository fields,
            InvoiceTemplateFieldGroupRepository groups,
            InvoiceTemplateDatasetRepository datasets,
            InvoiceTemplateCatalogReleaseRepository releases,
            ObjectMapper mapper) {
        this.fields = fields;
        this.groups = groups;
        this.datasets = datasets;
        this.releases = releases;
        this.mapper = mapper;
        this.fixed = null;
    }

    public InvoiceFieldCatalog(InvoiceCatalogSnapshot fixed) {
        this.fixed = fixed.withDerivedScopes();
        this.fields = null;
        this.groups = null;
        this.datasets = null;
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
            return mapper.readValue(release.getSnapshotJson(), InvoiceCatalogSnapshot.class)
                    .withDerivedScopes();
        } catch (Exception ex) {
            throw new IllegalStateException("Không đọc được danh mục", ex);
        }
    }

    private InvoiceCatalogSnapshot working() {
        var groupRows = groups.findAllByOrderBySortOrderAscIdAsc();
        var datasetRows = datasets.findAll(org.springframework.data.domain.Sort.by("id"));
        Map<Long, String> groupCodes = new HashMap<>();
        Map<Long, String> datasetCodes = new HashMap<>();
        groupRows.forEach(group -> groupCodes.put(group.getId(), group.getCode()));
        datasetRows.forEach(dataset -> datasetCodes.put(dataset.getId(), dataset.getCode()));
        var groupDefinitions =
                groupRows.stream()
                        .map(
                                group ->
                                        new InvoiceCatalogSnapshot.Group(
                                                group.getId(),
                                                group.getCode(),
                                                group.getLabel(),
                                                group.getSortOrder(),
                                                group.isActive(),
                                                group.getLockVersion()))
                        .toList();
        var datasetDefinitions =
                datasetRows.stream()
                        .map(
                                dataset ->
                                        new InvoiceCatalogSnapshot.Dataset(
                                                dataset.getId(),
                                                dataset.getCode(),
                                                dataset.getLabel(),
                                                datasetCodes.get(dataset.getParentId()),
                                                dataset.getCardinality(),
                                                dataset.getRootPath(),
                                                dataset.isActive(),
                                                dataset.getLockVersion()))
                        .toList();
        var structure =
                new InvoiceCatalogSnapshot(
                        "CURRENT", groupDefinitions, datasetDefinitions, List.of());
        List<InvoiceCatalogSnapshot.Field> definitions = new ArrayList<>();
        for (var field : fields.findAllByOrderBySortOrderAscIdAsc()) {
            String dataset = datasetCodes.get(field.getDatasetId());
            definitions.add(
                    new InvoiceCatalogSnapshot.Field(
                            field.getId(),
                            field.getFieldKey(),
                            field.getLabel(),
                            groupCodes.get(field.getGroupId()),
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
