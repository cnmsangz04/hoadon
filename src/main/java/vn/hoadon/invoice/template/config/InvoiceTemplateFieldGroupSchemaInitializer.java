package vn.hoadon.invoice.template.config;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Migrates the old group tree to the single-table catalog model.
 *
 * A group is now identified by its own absolute XML root.  There is no
 * database relationship between groups and no separate dataset table.
 */
@Component
public class InvoiceTemplateFieldGroupSchemaInitializer {
    private final JdbcTemplate jdbc;

    public InvoiceTemplateFieldGroupSchemaInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void migrate() {
        try {
            if (!tableExists("invoice_template_field_groups")) return;

            jdbc.update("update invoice_template_field_groups set root_path = case code "
                    + "when 'common' then '/HDon/DLHDon/TTChung' "
                    + "when 'related' then '/HDon/DLHDon/TTChung/TTHDLQuan' "
                    + "when 'seller' then '/HDon/DLHDon/NDHDon/NBan' "
                    + "when 'buyer' then '/HDon/DLHDon/NDHDon/NMua' "
                    + "when 'items' then '/HDon/DLHDon/NDHDon/DSHHDVu/HHDVu' "
                    + "when 'items.special' then '/HDon/DLHDon/NDHDon/DSHHDVu/HHDVu/TTHHDTrung/TTin' "
                    + "when 'taxGroups' then '/HDon/DLHDon/NDHDon/TToan/THTTLTSuat/LTSuat' "
                    + "when 'totals' then '/HDon/DLHDon/NDHDon/TToan' "
                    + "when 'fees' then '/HDon/DLHDon/NDHDon/TToan/DSLPhi/LPhi' "
                    + "when 'invoice' then '/HDon' "
                    + "when 'presentation' then '/HDon' "
                    + "else root_path end");

            replacePrefix("common", "DLHDon/TTChung/");
            replacePrefix("related", "DLHDon/TTChung/TTHDLQuan/");
            replacePrefix("seller", "DLHDon/NDHDon/NBan/");
            replacePrefix("buyer", "DLHDon/NDHDon/NMua/");
            replacePrefix("totals", "DLHDon/NDHDon/TToan/");

            // Remove the retired non-XML field type from the catalog.
            jdbc.update("delete from invoice_template_fields where source_type = 'CONTEXT'");
            ensureInvoiceOtherGroup();
            migrateFieldKeysBySourcePath();
            ensureInvoiceFields();

            if (columnExists("invoice_template_field_groups", "parent_id")) {
                var constraints = jdbc.queryForList(
                        "select fk.name from sys.foreign_keys fk "
                                + "join sys.foreign_key_columns fkc on fkc.constraint_object_id = fk.object_id "
                                + "join sys.columns c on c.object_id = fkc.parent_object_id and c.column_id = fkc.parent_column_id "
                                + "where fkc.parent_object_id = object_id('invoice_template_field_groups') "
                                + "and c.name = 'parent_id'",
                        String.class);
                for (String constraint : constraints) {
                    jdbc.execute("alter table invoice_template_field_groups drop constraint ["
                            + constraint.replace("]", "]]" ) + "]");
                }
                jdbc.execute("alter table invoice_template_field_groups drop column parent_id");
            }

            jdbc.update("delete from invoice_template_field_groups "
                    + "where code = 'document' "
                    + "and not exists (select 1 from invoice_template_fields where group_id = invoice_template_field_groups.id)");
        } catch (Exception ex) {
            throw new IllegalStateException("Không thể nâng cấp danh mục bảng dữ liệu hóa đơn", ex);
        }
    }

    private void replacePrefix(String group, String prefix) {
        jdbc.update("update f set source_path = substring(source_path, ?, 700) "
                        + "from invoice_template_fields f "
                        + "join invoice_template_field_groups g on g.id = f.group_id "
                        + "where g.code = ? and f.source_type = 'XML' and source_path like ?",
                prefix.length() + 1, group, prefix + "%");
    }

    private void ensureInvoiceFields() {
        Long groupId = jdbc.query(
                "select id from invoice_template_field_groups where code = 'invoice'",
                rs -> rs.next() ? rs.getLong(1) : null);
        if (groupId == null) {
            Integer sortOrder = jdbc.queryForObject(
                    "select coalesce(max(sort_order), 0) + 10 from invoice_template_field_groups",
                    Integer.class);
            jdbc.update(
                    "insert into invoice_template_field_groups "
                            + "(code, label, cardinality, root_path, sort_order, active, lock_version) "
                            + "values ('invoice', N'Thông tin hóa đơn', 'ONE', '/HDon', ?, 1, 0)",
                    sortOrder == null ? 10 : sortOrder);
            groupId = jdbc.queryForObject(
                    "select id from invoice_template_field_groups where code = 'invoice'",
                    Long.class);
        }

        Long otherGroupId = jdbc.query(
                "select id from invoice_template_field_groups where code = 'invoice.other'",
                rs -> rs.next() ? rs.getLong(1) : null);
        if (otherGroupId == null) throw new IllegalStateException("Không tạo được nhóm invoice.other");

        ensureInvoiceField(groupId, "invoice.MCCQT", "Mã của cơ quan thuế", "MCCQT",
                "Mã cơ quan thuế trên XML hóa đơn");
        ensureInvoiceField(otherGroupId, "invoice.other.TTinMTCuu.DLieu", "Mã tra cứu hóa đơn",
                "DLHDon/TTKhac/TTin[TTruong='MTCuu']/DLieu",
                "Mã tra cứu trong HDon/DLHDon/TTKhac");
    }

    private void ensureInvoiceField(Long groupId, String key, String label, String sourcePath, String description) {
        Integer count = jdbc.queryForObject(
                "select count(*) from invoice_template_fields where field_key = ?", Integer.class, key);
        if (count != null && count > 0) return;
        Integer sortOrder = jdbc.queryForObject(
                "select coalesce(max(sort_order), 0) + 10 from invoice_template_fields",
                Integer.class);
        jdbc.update(
                "insert into invoice_template_fields "
                        + "(field_key, label, group_id, source_type, source_path, value_type, max_length, "
                        + "default_formatter, description, required_display, active, sort_order, lock_version, "
                        + "created_at, updated_at) "
                        + "values (?, ?, ?, 'XML', ?, 'STRING', 255, 'plainText', ?, 0, 1, ?, 0, SYSUTCDATETIME(), SYSUTCDATETIME())",
                key, label, groupId, sourcePath, description, sortOrder == null ? 10 : sortOrder);
    }

    private void ensureInvoiceOtherGroup() {
        Long groupId = jdbc.query(
                "select id from invoice_template_field_groups where code = ?",
                rs -> rs.next() ? rs.getLong(1) : null,
                "invoice.other");
        if (groupId == null) {
            Integer sortOrder = jdbc.queryForObject(
                    "select coalesce(max(sort_order), 0) + 10 from invoice_template_field_groups",
                    Integer.class);
            jdbc.update(
                    "insert into invoice_template_field_groups "
                            + "(code, label, cardinality, root_path, sort_order, active, lock_version) "
                            + "values (?, N'Thông tin khác', 'ONE', ?, ?, 1, 0)",
                    "invoice.other", "/HDon/DLHDon/TTKhac", sortOrder == null ? 10 : sortOrder);
            groupId = jdbc.query(
                    "select id from invoice_template_field_groups where code = ?",
                    rs -> rs.next() ? rs.getLong(1) : null,
                    "invoice.other");
        }
        if (groupId == null) throw new IllegalStateException("Không tạo được nhóm invoice.other");

        jdbc.update(
                "update invoice_template_fields set group_id = ?, source_path = ? "
                        + "where field_key in ('invoice.lookupCode', 'invoice.other.maTraCuu', 'invoice.other.TTinMTCuu.DLieu') "
                        + "or source_path = ?",
                groupId, "TTin[TTruong='MTCuu']/DLieu", "TTin[TTruong='MTCuu']/DLieu");
    }

    private void migrateFieldKeysBySourcePath() {
        List<FieldRow> fields = jdbc.query(
                "select f.id, f.field_key, g.code, f.source_path "
                        + "from invoice_template_fields f "
                        + "join invoice_template_field_groups g on g.id = f.group_id "
                        + "where f.source_type = 'XML'",
                (rs, rowNum) -> new FieldRow(
                        rs.getLong("id"), rs.getString("field_key"),
                        rs.getString("code"), rs.getString("source_path")));

        for (FieldRow field : fields) {
            String expectedKey = fieldKey(field.group(), field.sourcePath());
            if (expectedKey.equals(field.key())) continue;
            Integer duplicate = jdbc.queryForObject(
                    "select count(*) from invoice_template_fields where field_key = ? and id <> ?",
                    Integer.class, expectedKey, field.id());
            if (duplicate != null && duplicate > 0) {
                throw new IllegalStateException("Trùng mã phần tử sau khi chuẩn hóa: " + expectedKey);
            }
            replaceDraftFieldKey(field.key(), expectedKey);
            jdbc.update("update invoice_template_fields set field_key = ? where id = ?",
                    expectedKey, field.id());
        }
    }

    private void replaceDraftFieldKey(String oldKey, String newKey) {
        jdbc.update("update invoice_template_drafts "
                        + "set source_content = replace(source_content, ?, ?), "
                        + "manifest_json = replace(manifest_json, ?, ?) "
                        + "where source_content like ? or manifest_json like ?",
                oldKey, newKey, oldKey, newKey, "%" + oldKey + "%", "%" + oldKey + "%");
    }

    private String fieldKey(String group, String sourcePath) {
        String prefix = groupIdentifier(group);
        if (sourcePath == null || sourcePath.isBlank()) {
            throw new IllegalStateException("Thiếu đường dẫn XML của phần tử " + group);
        }
        String path = Arrays.stream(sourcePath.trim().split("/"))
                .map(this::pathSegmentIdentifier)
                .filter(value -> !value.isBlank())
                .reduce((left, right) -> left + "." + right)
                .orElseThrow(() -> new IllegalStateException("Đường dẫn XML không hợp lệ: " + sourcePath));
        return prefix + "." + path;
    }

    private String groupIdentifier(String value) {
        String normalized = normalize(value);
        StringBuilder result = new StringBuilder();
        for (String segment : normalized.split("\\.")) {
            String clean = segment.replaceAll("[^A-Za-z0-9]", "");
            if (clean.isBlank()) continue;
            if (result.length() > 0) result.append('.');
            result.append(Character.toLowerCase(clean.charAt(0))).append(clean.substring(1));
        }
        return result.toString();
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

    private String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.replace('đ', 'd').replace('Đ', 'D'), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }

    private record FieldRow(Long id, String key, String group, String sourcePath) {}

    private boolean tableExists(String table) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select cast(case when object_id(?) is null then 0 else 1 end as bit)",
                Boolean.class, table));
    }

    private boolean columnExists(String table, String column) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select cast(case when exists (select 1 from sys.columns where object_id = object_id(?) and name = ?) then 1 else 0 end as bit)",
                Boolean.class, table, column));
    }
}
