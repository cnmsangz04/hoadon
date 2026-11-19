package vn.hoadon.invoice.template.catalog;

import vn.hoadon.invoice.template.service.TemplateException;

import java.util.*;

public final class InvoiceCatalogValidator {
    private InvoiceCatalogValidator() {}

    private static final String XML_PATH_SEGMENT =
            "[A-Za-z_][A-Za-z0-9_.-]*(?:\\[[A-Za-z_][A-Za-z0-9_.-]*=(?:'[^']*'|\"[^\"]*\")\\])?";
    private static final String XML_RELATIVE_PATH = XML_PATH_SEGMENT + "(?:/" + XML_PATH_SEGMENT + ")*";

    public static void validate(InvoiceCatalogSnapshot source) {
        boolean legacyHierarchy = source.datasets().stream().anyMatch(dataset -> dataset.parent() != null);
        for (var group : source.groups()) {
            if (!Set.of("ONE", "MANY").contains(group.cardinality()))
                fail("Kiểu bảng dữ liệu không hợp lệ");
            if (group.rootPath() == null
                    || !group.rootPath().matches("/HDon(?:/[A-Za-z_][A-Za-z0-9_.-]*)*"))
                fail("Đường dẫn XML của bảng dữ liệu không hợp lệ");
        }
        for (var dataset : source.datasets()) {
            if (legacyHierarchy) {
                Set<String> seen = new HashSet<>();
                var current = dataset;
                while (current != null) {
                    if (!seen.add(current.code()) || seen.size() > 16)
                        fail("Danh sách cha có vòng hoặc quá sâu");
                    current = current.parent() == null ? null : source.requireDataset(current.parent());
                }
                if (dataset.parent() == null
                        && (!"document".equals(dataset.code())
                                || !"ONE".equals(dataset.cardinality())
                                || !"/HDon".equals(dataset.rootPath())))
                    fail("Root phải là document tại /HDon");
            }
            if (!Set.of("ONE", "MANY").contains(dataset.cardinality()))
                fail("Kiểu danh sách không hợp lệ");
            if (dataset.rootPath() == null
                    || !dataset.rootPath()
                            .matches(
                                    legacyHierarchy
                                            ? dataset.parent() == null
                                                    ? "/HDon"
                                                    : "[A-Za-z_][A-Za-z0-9_.-]*(?:/[A-Za-z_][A-Za-z0-9_.-]*)*"
                                            : "/HDon(?:/[A-Za-z_][A-Za-z0-9_.-]*)*"))
                fail(legacyHierarchy
                        ? "Đường dẫn danh sách không hợp lệ"
                        : "Đường dẫn bảng dữ liệu phải là đường dẫn tuyệt đối bắt đầu bằng /HDon");
        }
        for (var field : source.fields()) {
            source.requireDataset(field.dataset());
            source.requireGroup(field.group());
            if (!legacyHierarchy && !source.groupRowScope(field.group()).equals(field.dataset()))
                fail("Phần tử không thuộc đúng phạm vi của nhóm");
            if (field.valueType() == null
                    || !Set.of("STRING", "INTEGER", "DECIMAL", "DATE", "BOOLEAN")
                            .contains(field.valueType())) fail("Kiểu dữ liệu không hợp lệ");
            if (field.maxLength() != null && field.maxLength() < 1) fail("Độ dài phải lớn hơn 0");
            if (field.sourcePath() == null || field.sourceType() == null)
                fail("Thiếu nguồn dữ liệu");
            if ("XML".equals(field.sourceType())) {
                if (!field.sourcePath()
                        .matches(XML_RELATIVE_PATH))
                    fail("Đường dẫn XML tương đối không hợp lệ");
            } else fail("Nguồn dữ liệu không hợp lệ");
            if (!field.active() && field.requiredDisplay())
                fail("Phần tử bắt buộc hiển thị phải được bật");
        }
    }

    private static void fail(String message) {
        throw TemplateException.invalid("INVALID_CATALOG", message);
    }
}
