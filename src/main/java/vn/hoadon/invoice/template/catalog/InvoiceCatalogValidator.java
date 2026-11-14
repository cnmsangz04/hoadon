package vn.hoadon.invoice.template.catalog;

import vn.hoadon.invoice.template.service.TemplateException;

import java.util.*;

public final class InvoiceCatalogValidator {
    private InvoiceCatalogValidator() {}

    public static void validate(InvoiceCatalogSnapshot source) {
        if (source.datasets().stream().filter(d -> d.parent() == null).count() != 1)
            fail("Cần đúng một root dữ liệu");
        for (var dataset : source.datasets()) {
            Set<String> seen = new HashSet<>();
            var current = dataset;
            while (current != null) {
                if (!seen.add(current.code()) || seen.size() > 16)
                    fail("Danh sách cha có vòng hoặc quá sâu");
                current = current.parent() == null ? null : source.requireDataset(current.parent());
            }
            if (!Set.of("ONE", "MANY").contains(dataset.cardinality()))
                fail("Kiểu danh sách không hợp lệ");
            if (dataset.parent() == null
                    && (!"document".equals(dataset.code())
                            || !"ONE".equals(dataset.cardinality())
                            || !"/HDon".equals(dataset.rootPath())))
                fail("Root phải là document tại /HDon");
            if (dataset.rootPath() == null
                    || !dataset.rootPath()
                            .matches(
                                    dataset.parent() == null
                                            ? "/HDon"
                                            : "[A-Za-z_][A-Za-z0-9_.-]*(?:/[A-Za-z_][A-Za-z0-9_.-]*)*"))
                fail("Đường dẫn danh sách không hợp lệ");
        }
        for (var field : source.fields()) {
            source.requireDataset(field.dataset());
            if (source.groups().stream().noneMatch(g -> g.code().equals(field.group())))
                fail("Nhóm không tồn tại");
            if (field.valueType() == null
                    || !Set.of("STRING", "INTEGER", "DECIMAL", "DATE", "BOOLEAN")
                            .contains(field.valueType())) fail("Kiểu dữ liệu không hợp lệ");
            if (field.maxLength() != null && field.maxLength() < 1) fail("Độ dài phải lớn hơn 0");
            if (field.sourcePath() == null || field.sourceType() == null)
                fail("Thiếu nguồn dữ liệu");
            if ("XML".equals(field.sourceType())) {
                if (!field.sourcePath()
                        .matches("[A-Za-z_][A-Za-z0-9_.-]*(?:/[A-Za-z_][A-Za-z0-9_.-]*)*"))
                    fail("Đường dẫn XML tương đối không hợp lệ");
            } else if ("CONTEXT".equals(field.sourceType())) {
                if (!"document".equals(source.rowScope(field.dataset()))
                        || !InvoiceExternalFieldRegistry.contextSupports(field.sourcePath()))
                    fail("Thông tin hệ thống chưa được hỗ trợ hoặc thuộc danh sách lặp");
            } else fail("Nguồn dữ liệu không hợp lệ");
            if (!field.active() && field.requiredDisplay())
                fail("Phần tử bắt buộc hiển thị phải được bật");
        }
    }

    private static void fail(String message) {
        throw TemplateException.invalid("INVALID_CATALOG", message);
    }
}
