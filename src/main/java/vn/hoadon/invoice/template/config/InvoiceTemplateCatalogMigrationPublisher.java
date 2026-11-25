package vn.hoadon.invoice.template.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import vn.hoadon.invoice.template.catalog.InvoiceFieldCatalog;
import vn.hoadon.invoice.template.catalog.InvoiceCatalogSnapshot;
import vn.hoadon.invoice.template.service.TemplateException;

/** Publishes one catalog release after the group-tree migration. */
@Component
public class InvoiceTemplateCatalogMigrationPublisher {
    private final InvoiceFieldCatalog catalog;
    private final JdbcTemplate jdbc;

    public InvoiceTemplateCatalogMigrationPublisher(InvoiceFieldCatalog catalog, JdbcTemplate jdbc) {
        this.catalog = catalog;
        this.jdbc = jdbc;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void publishMigratedCatalog() {
        final InvoiceCatalogSnapshot latest;
        try {
            latest = catalog.latest();
        } catch (TemplateException ex) {
            if ("CATALOG_MISSING".equals(ex.getCode())) return;
            throw ex;
        }
        boolean oldGroupPaths = latest.groups().stream()
                .filter(group -> !"document".equals(group.code()))
                .anyMatch(group -> group.rootPath() == null || !group.rootPath().startsWith("/HDon/"));
        boolean retiredSystemFields = latest.fields().stream()
                .anyMatch(field -> "CONTEXT".equals(field.sourceType()));
        boolean missingCompatibilityFields = latest.fields().stream()
                .noneMatch(field -> "invoice.MCCQT".equals(field.key()))
                || latest.fields().stream()
                .noneMatch(field -> "invoice.other.TTinMTCuu.DLieu".equals(field.key()));
        boolean missingSignatureDate = latest.fields().stream()
                .noneMatch(field -> "invoice.DSCKS.NBan.Signature.SignedAt".equals(field.key()));
        boolean legacySignatureDate = latest.fields().stream()
                .anyMatch(field -> "invoice.signature.SignedAt".equals(field.key()));
        boolean missingInvoiceOtherGroup = latest.groups().stream()
                .noneMatch(group -> "invoice.other".equals(group.code()));
        boolean lookupCodeInWrongGroup = latest.fields().stream()
                .filter(field -> "invoice.other.TTinMTCuu.DLieu".equals(field.key()))
                .anyMatch(field -> !"invoice.other".equals(field.group())
                        || !"TTin[TTruong='MTCuu']/DLieu".equals(field.sourcePath()));
        boolean legacyFieldKeys = latest.fields().stream()
                .anyMatch(field -> "invoice.codeCqt".equals(field.key())
                        || "invoice.lookupCode".equals(field.key())
                        || "invoice.other.maTraCuu".equals(field.key()));
        var stockIssueKeys = java.util.Set.of(
                "seller.LDDNBo", "seller.HDKTSo", "seller.HDKTNgay", "seller.HVTNXHang",
                "seller.TNVChuyen", "seller.PTVChuyen", "seller.HDSo", "buyer.HVTNNHang");
        boolean missingStockIssueFields = !latest.fields().stream()
                .map(InvoiceCatalogSnapshot.Field::key)
                .collect(java.util.stream.Collectors.toSet())
                .containsAll(stockIssueKeys);
        var salesInvoiceKeys = java.util.Set.of("common.HDDCKPTQuan", "items.MNNNghe");
        boolean missingSalesInvoiceFields = !latest.fields().stream()
                .map(InvoiceCatalogSnapshot.Field::key)
                .collect(java.util.stream.Collectors.toSet())
                .containsAll(salesInvoiceKeys);
        boolean missingInvoiceTypeScopes = latest.fields().stream()
                .anyMatch(field -> field.applicableInvoiceTypes() == null
                        || field.applicableInvoiceTypes().isBlank());
        var invoicePaymentKeys = java.util.Set.of(
                "common.HTTToan", "totals.TgTTTBSo", "totals.TgTTTBChu");
        boolean stockIssuePaymentFields = latest.fields().stream()
                .filter(field -> invoicePaymentKeys.contains(field.key()))
                .anyMatch(field -> field.supportsInvoiceType(6, 1)
                        || field.supportsInvoiceType(6, 2));
        if (oldGroupPaths || retiredSystemFields || missingCompatibilityFields || missingSignatureDate || legacySignatureDate || missingInvoiceOtherGroup
                || lookupCodeInWrongGroup || legacyFieldKeys || missingStockIssueFields
                || missingSalesInvoiceFields || missingInvoiceTypeScopes || stockIssuePaymentFields) {
            catalog.checkpoint(null);
            jdbc.update("update invoice_template_drafts set field_catalog_version = ?", catalog.version());
        }
    }
}
