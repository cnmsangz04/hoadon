package vn.hoadon.invoice.template.config;

import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.jdbc.core.ConnectionCallback;

@Component
public class InvoiceTemplateFieldGroupSchemaInitializer {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public InvoiceTemplateFieldGroupSchemaInitializer(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    @PostConstruct
    public void migrate() {
        try {
            if (!isSqlServer()) return;
            if (!tableExists("invoice_template_field_groups")) return;

            boolean initializeInvoiceTypeScopes = ensureApplicableInvoiceTypesColumn();

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

            jdbc.update("delete from invoice_template_fields where source_type = 'CONTEXT'");
            ensureInvoiceOtherGroup();
            removeLegacySignatureDateField();
            migrateSignatureBlocks();
            ensureInvoiceFields();
            ensureLineAmountField();
            removeLegacyLineAmountField();
            migrateFieldKeysBySourcePath();
            ensureSalesInvoiceFields();
            ensureStockIssueFields();
            removeLegacyStockQuantityFields();
            if (initializeInvoiceTypeScopes) applyInvoiceTypeScopes();
            correctInvoiceTypeScopes();

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
        ensureInvoiceField(groupId, "invoice.DSCKS.NBan.Signature.SignedAt", "Ngày ký số",
                "DSCKS/NBan/Signature/SignedAt",
                "Ngày ký số trong HDon/DSCKS/NBan/Signature; được Java định dạng trước khi hiển thị");
        ensureInvoiceField(otherGroupId, "invoice.other.TTinMTCuu.DLieu", "Mã tra cứu hóa đơn",
                "TTin[TTruong='MTCuu']/DLieu",
                "Mã tra cứu trong HDon/DLHDon/TTKhac");
    }

    private void ensureStockIssueFields() {
        ensureGroupField("seller", "seller.LDDNBo", "Lệnh điều động nội bộ", "LDDNBo", "STRING", 255, "plainText", "6:1", "Chỉ tiêu phiếu xuất kho kiêm vận chuyển nội bộ");
        ensureGroupField("seller", "seller.HDKTSo", "Số hợp đồng kinh tế", "HDKTSo", "STRING", 255, "plainText", "6:2", "Chỉ tiêu phiếu xuất kho hàng gửi bán đại lý");
        ensureGroupField("seller", "seller.HDKTNgay", "Ngày hợp đồng kinh tế", "HDKTNgay", "DATE", 10, "date", "6:2", "Chỉ tiêu phiếu xuất kho hàng gửi bán đại lý");
        ensureGroupField("seller", "seller.HVTNXHang", "Họ tên người xuất hàng", "HVTNXHang", "STRING", 100, "plainText", "6", "Chỉ tiêu phiếu xuất kho");
        ensureGroupField("seller", "seller.TNVChuyen", "Tên người vận chuyển", "TNVChuyen", "STRING", 100, "plainText", "6", "Chỉ tiêu phiếu xuất kho");
        ensureGroupField("seller", "seller.PTVChuyen", "Phương tiện vận chuyển", "PTVChuyen", "STRING", 50, "plainText", "6", "Chỉ tiêu phiếu xuất kho");
        ensureGroupField("seller", "seller.HDSo", "Số hợp đồng vận chuyển", "HDSo", "STRING", 255, "plainText", "6", "Chỉ tiêu phiếu xuất kho");
        ensureGroupField("buyer", "buyer.HVTNNHang", "Họ tên người nhận hàng", "HVTNNHang", "STRING", 100, "plainText", "6", "Chỉ tiêu phiếu xuất kho");
        ensureGroupField("items", "items.SLuongThucXuat", "Số lượng thực xuất", "TTKhac/TTin[TTruong='SLuongThucXuat']/DLieu", "DECIMAL", 21, "decimal", "6:1", "Số lượng thực xuất của phiếu xuất kho kiêm vận chuyển nội bộ");
        ensureGroupField("items", "items.SLuongThucNhap", "Số lượng thực nhập", "TTKhac/TTin[TTruong='SLuongThucNhap']/DLieu", "DECIMAL", 21, "decimal", "6:1", "Số lượng thực nhập của phiếu xuất kho kiêm vận chuyển nội bộ");
    }

    private void ensureSalesInvoiceFields() {
        ensureGroupField("common", "common.HDDCKPTQuan", "Hóa đơn dành cho khu phi thuế quan", "HDDCKPTQuan", "INTEGER", 1, "plainText", "2", "Chỉ tiêu hóa đơn bán hàng");
        ensureGroupField("items", "items.MNNNghe", "Mã nhóm ngành nghề", "MNNNghe", "STRING", 4, "plainText", "2", "Chỉ tiêu hóa đơn bán hàng");
    }

    private void ensureLineAmountField() {
        ensureGroupField("items", "items.TCong", "Tổng", "TTKhac/TTin[TTruong='TCong']/DLieu",
                "DECIMAL", 21, "decimal", "1,2,6", "Tổng tiền của dòng hàng hóa, dịch vụ");
    }

    private void ensureGroupField(String groupCode, String key, String label, String sourcePath,
                                  String valueType, Integer maxLength, String formatter,
                                  String applicableInvoiceTypes, String description) {
        Integer count = jdbc.queryForObject(
                "select count(*) from invoice_template_fields where field_key = ?", Integer.class, key);
        if (count != null && count > 0) return;
        Long groupId = jdbc.query(
                "select id from invoice_template_field_groups where code = ?",
                rs -> rs.next() ? rs.getLong(1) : null,
                groupCode);
        if (groupId == null) throw new IllegalStateException("Không tìm thấy nhóm phần tử " + groupCode);
        Integer sortOrder = jdbc.queryForObject(
                "select coalesce(max(sort_order), 0) + 10 from invoice_template_fields",
                Integer.class);
        jdbc.update(
                "insert into invoice_template_fields "
                        + "(field_key, label, group_id, source_type, source_path, value_type, max_length, "
                        + "default_formatter, applicable_invoice_types, description, required_display, active, sort_order, lock_version, "
                        + "created_at, updated_at) "
                        + "values (?, ?, ?, 'XML', ?, ?, ?, ?, ?, ?, 0, 1, ?, 0, SYSUTCDATETIME(), SYSUTCDATETIME())",
                key, label, groupId, sourcePath, valueType, maxLength, formatter,
                applicableInvoiceTypes, description, sortOrder == null ? 10 : sortOrder);
    }

    private boolean ensureApplicableInvoiceTypesColumn() {
        boolean initialize = false;
        if (!columnExists("invoice_template_fields", "applicable_invoice_types")) {
            jdbc.execute("alter table invoice_template_fields add applicable_invoice_types varchar(32) null");
            initialize = true;
        }
        Integer missing = jdbc.queryForObject(
                "select count(*) from invoice_template_fields where applicable_invoice_types is null "
                        + "or ltrim(rtrim(applicable_invoice_types)) = ''",
                Integer.class);
        return initialize || missing != null && missing > 0;
    }

    private void applyInvoiceTypeScopes() {
        setInvoiceTypes("1,2,6",
                "common.PBan", "common.THDon", "common.KHMSHDon", "common.KHHDon", "common.SHDon",
                "common.NLap", "common.DVTTe", "common.TGia", "common.MSTTCGP",
                "related.TCHDon", "related.MSTCLQuan", "related.LHDCLQuan", "related.KHMSHDCLQuan",
                "related.KHHDCLQuan", "related.SHDCLQuan", "related.NLHDCLQuan", "related.LDDCTThe",
                "related.GChu", "seller.Ten", "seller.MST", "seller.DChi", "seller.MDDKDoanh",
                "seller.TDDKDoanh", "seller.DCDDKDoanh", "buyer.Ten", "buyer.MST", "buyer.DChi",
                "items.TChat", "items.STT", "items.MHHDVu", "items.THHDVu", "items.DVTinh",
                "items.SLuong", "items.DGia", "items.ThTien", "items.TCong",
                "invoice.MCCQT", "invoice.DSCKS.NBan.Signature.SignedAt", "invoice.other.TTinMTCuu.DLieu");

        setInvoiceTypes("1", "common.HDCTTChinh", "items.TSuat", "taxGroups.TSuat",
                "taxGroups.ThTien", "taxGroups.TThue", "totals.TgTCThue", "totals.TGTKCThue",
                "totals.TgTThue");
        setInvoiceTypes("2", "common.HDDCKPTQuan", "items.MNNNghe");
        setInvoiceTypes("6:1", "seller.LDDNBo");
        setInvoiceTypes("6:1", "items.SLuongThucXuat", "items.SLuongThucNhap");
        setInvoiceTypes("6:2", "seller.HDKTSo", "seller.HDKTNgay");
        setInvoiceTypes("6", "seller.HVTNXHang", "seller.TNVChuyen", "seller.PTVChuyen",
                "seller.HDSo", "buyer.HVTNNHang");
        jdbc.update("update invoice_template_fields set applicable_invoice_types = '1,2' "
                + "where applicable_invoice_types is null or ltrim(rtrim(applicable_invoice_types)) = ''");
    }

    private void setInvoiceTypes(String invoiceTypes, String... keys) {
        for (String key : keys) {
            jdbc.update("update invoice_template_fields set applicable_invoice_types = ? where field_key = ? "
                            + "and (applicable_invoice_types is null or ltrim(rtrim(applicable_invoice_types)) = '')",
                    invoiceTypes, key);
        }
    }

    private void correctInvoiceTypeScopes() {
        jdbc.update("update invoice_template_fields set applicable_invoice_types = '1,2' "
                + "where field_key in ('common.HTTToan', 'totals.TgTTTBSo', 'totals.TgTTTBChu') "
                + "and applicable_invoice_types = '1,2,6'");
    }

    private void removeLegacySignatureDateField() {
        jdbc.update("update invoice_template_drafts set source_content = replace(source_content, ?, ?), manifest_json = replace(manifest_json, ?, ?) where source_content like ? or manifest_json like ?",
                "invoice.signature.SignedAt", "invoice.DSCKS.NBan.Signature.SignedAt",
                "invoice.signature.SignedAt", "invoice.DSCKS.NBan.Signature.SignedAt",
                "%invoice.signature.SignedAt%", "%invoice.signature.SignedAt%");
        jdbc.update("delete from invoice_template_fields where field_key = ?", "invoice.signature.SignedAt");
    }

    private void migrateSignatureBlocks() {
        var rows = jdbc.query(
                "select id, source_content from invoice_template_drafts where source_content like '%signatureSummary%'",
                (rs, rowNum) -> new LayoutRow(rs.getLong("id"), rs.getString("source_content")));
        for (LayoutRow row : rows) {
            try {
                JsonNode layout = mapper.readTree(row.sourceContent());
                boolean changed = addSignatureChildren(layout.path("body"));
                if (changed) {
                    jdbc.update("update invoice_template_drafts set source_content = ? where id = ?",
                            mapper.writeValueAsString(layout), row.id());
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Không thể cập nhật khối thông tin chữ ký số", ex);
            }
        }
    }

    private boolean addSignatureChildren(JsonNode node) {
        if (node == null || !node.isObject()) return false;
        boolean changed = false;
        if ("signatureSummary".equals(node.path("type").asText())) {
            String id = node.path("id").asText("signature");
            ObjectNode object = (ObjectNode) node;
            if (!node.has("children")) {
                ArrayNode children = mapper.createArrayNode();
                children.add(textBlock(id + "Signature", "Signature", true));
                children.add(textBlock(id + "By", "Ký bởi:", false));
                children.add(fieldBlock(id + "Signer", "seller.Ten", ""));
                children.add(fieldBlock(id + "SignedAt", "invoice.DSCKS.NBan.Signature.SignedAt", "Ký ngày"));
                object.set("children", children);
                changed = true;
            }
            if (!node.has("widthMm")) {
                object.put("widthMm", 62);
                changed = true;
            }
            if (!node.has("heightMm")) {
                object.put("heightMm", 24);
                changed = true;
            }
            ObjectNode style = object.with("style");
            if (!style.has("borderWidthMm")) {
                style.put("borderWidthMm", 0.25);
                changed = true;
            }
            if (!style.has("borderColor")) {
                style.put("borderColor", "#174a8b");
                changed = true;
            }
            if (!style.has("paddingMm")) {
                style.put("paddingMm", 2);
                changed = true;
            }
        }
        JsonNode children = node.path("children");
        if (children.isArray()) for (JsonNode child : children) changed |= addSignatureChildren(child);
        return changed;
    }

    private ObjectNode textBlock(String id, String text, boolean bold) {
        ObjectNode block = mapper.createObjectNode();
        block.put("id", id);
        block.put("type", "text");
        block.put("text", text);
        ObjectNode style = block.putObject("style");
        style.put("color", "#ff0000");
        if (bold) style.put("fontWeight", 700);
        return block;
    }

    private ObjectNode fieldBlock(String id, String key, String label) {
        ObjectNode block = mapper.createObjectNode();
        block.put("id", id);
        block.put("type", "field");
        block.put("fieldKey", key);
        block.put("formatter", "plainText");
        block.put("label", label);
        block.putObject("style").put("color", "#ff0000");
        return block;
    }

    private record LayoutRow(Long id, String sourceContent) {}

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
                        + "default_formatter, applicable_invoice_types, description, required_display, active, sort_order, lock_version, "
                        + "created_at, updated_at) "
                        + "values (?, ?, ?, 'XML', ?, 'STRING', 255, 'plainText', '1,2,6', ?, 0, 1, ?, 0, SYSUTCDATETIME(), SYSUTCDATETIME())",
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
            if (isCanonicalStockQuantityField(field.key())) continue;
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

    private void removeLegacyStockQuantityFields() {
        removeLegacyFieldKey("items.TTKhac.TTinSLuongThucXuat.DLieu", "items.SLuongThucXuat");
        removeLegacyFieldKey("items.TTKhac.TTinSLuongThucNhap.DLieu", "items.SLuongThucNhap");
    }

    private void removeLegacyLineAmountField() {
        removeLegacyFieldKey("items.TTKhac.TTinTCong.DLieu", "items.TCong");
    }

    private void removeLegacyFieldKey(String legacyKey, String canonicalKey) {
        Integer canonicalCount = jdbc.queryForObject(
                "select count(*) from invoice_template_fields where field_key = ?", Integer.class, canonicalKey);
        if (canonicalCount == null || canonicalCount == 0) return;
        replaceDraftFieldKey(legacyKey, canonicalKey);
        jdbc.update("delete from invoice_template_fields where field_key = ?", legacyKey);
    }

    private boolean isCanonicalStockQuantityField(String key) {
        return "items.SLuongThucXuat".equals(key) || "items.SLuongThucNhap".equals(key);
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

    private boolean isSqlServer() {
        return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection ->
                connection.getMetaData().getDatabaseProductName().toLowerCase().contains("sql server")));
    }

    private boolean columnExists(String table, String column) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select cast(case when exists (select 1 from sys.columns where object_id = object_id(?) and name = ?) then 1 else 0 end as bit)",
                Boolean.class, table, column));
    }
}
