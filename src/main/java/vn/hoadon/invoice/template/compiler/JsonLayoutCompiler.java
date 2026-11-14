package vn.hoadon.invoice.template.compiler;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import vn.hoadon.invoice.template.catalog.InvoiceFieldCatalog;
import vn.hoadon.invoice.template.catalog.InvoiceCatalogSnapshot;
import vn.hoadon.invoice.template.service.TemplateException;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Component
public class JsonLayoutCompiler {
    public static final String SCHEMA_VERSION = "invoice-layout/1";
    public static final String COMPILER_VERSION = "layout-compiler/3";
    private static final Set<String> FORMATTERS = Set.of("plainText", "moneyByCurrency", "decimal", "date", "invoiceNumber");
    private static final Set<String> TYPES = Set.of("stack", "columns", "text", "field", "image", "itemsTable", "taxSummaryTable", "feesTable", "relatedInvoice", "totals", "signatureSummary", "divider", "spacer", "pageBreak", "table");
    private final InvoiceFieldCatalog catalog;

    public JsonLayoutCompiler(InvoiceFieldCatalog catalog) { this.catalog = catalog; }

    public InvoiceCatalogSnapshot catalogSnapshot(JsonNode layout) { return catalog.snapshot(layout.path("fieldCatalogVersion").asText()); }

    public Set<String> validate(JsonNode layout, boolean publishing) {
        properties(layout, Set.of("schemaVersion", "fieldCatalogVersion", "page", "theme", "fontFamilies", "assets", "body"));
        if (!SCHEMA_VERSION.equals(layout.path("schemaVersion").asText()) || layout.path("fieldCatalogVersion").asText().isBlank()) {
            fail("Phiên bản layout hoặc danh mục trường không được hỗ trợ");
        }
        var definitions = catalogSnapshot(layout);
        JsonNode page = layout.path("page");
        properties(page, Set.of("format", "orientation", "marginMm"));
        choice(page, "format", Set.of("A4", "A5"), "A4");
        choice(page, "orientation", Set.of("portrait", "landscape"), "portrait");
        JsonNode margin = page.path("marginMm");
        properties(margin, Set.of("top", "right", "bottom", "left"));
        for (String side : List.of("top", "right", "bottom", "left")) number(margin, side, 3, 40, 10);
        JsonNode theme = layout.path("theme");
        properties(theme, Set.of("fontAssetFamily", "fontSizePt", "lineHeight", "color"));
        number(theme, "fontSizePt", 7, 32, 10);
        number(theme, "lineHeight", 1, 2, 1.3);
        color(theme, "color");
        if (!Set.of("body", "sans").contains(theme.path("fontAssetFamily").asText("body"))) fail("Font chưa được đăng ký");
        if (!layout.path("assets").isObject()) fail("Danh sách tài nguyên không hợp lệ");
        if (layout.path("assets").size() > 100) fail("Mẫu có quá nhiều tài nguyên");
        layout.path("assets").fields().forEachRemaining(entry -> {
            if (!entry.getKey().matches("[a-zA-Z][a-zA-Z0-9_-]{0,63}")) fail("Tên tài nguyên không hợp lệ");
            properties(entry.getValue(), Set.of("assetId"));
            if (!entry.getValue().path("assetId").canConvertToLong() || entry.getValue().path("assetId").asLong() < 1) fail("Asset ID không hợp lệ");
        });
        JsonNode families = layout.path("fontFamilies");
        if (families.isObject()) {
            families.fields().forEachRemaining(entry -> {
                if (!Set.of("body", "sans").contains(entry.getKey())) fail("Font family không được hỗ trợ");
                properties(entry.getValue(), Set.of("regularAsset", "boldAsset", "italicAsset", "boldItalicAsset"));
                entry.getValue().elements().forEachRemaining(value -> {
                    if (!value.isTextual() || !layout.path("assets").has(value.asText())) fail("Font thiếu tài nguyên");
                });
                if (!entry.getValue().has("regularAsset") || !entry.getValue().has("boldAsset") || !entry.getValue().has("italicAsset") || !entry.getValue().has("boldItalicAsset")) fail("Font tùy chỉnh cần đủ regular, bold, italic và bold-italic");
            });
        } else if (!families.isMissingNode()) fail("Cấu hình font không hợp lệ");
        Set<String> ids = new HashSet<>();
        Set<String> bindings = new HashSet<>();
        validateBlock(layout.path("body"), layout, ids, bindings, "", 0, false, definitions);
        Set<String> required = definitions.fields().stream().filter(InvoiceCatalogSnapshot.Field::active)
                .filter(InvoiceCatalogSnapshot.Field::requiredDisplay).map(InvoiceCatalogSnapshot.Field::key)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        if (publishing && !bindings.containsAll(required)) {
            Set<String> missing = new TreeSet<>(required);
            missing.removeAll(bindings);
            throw TemplateException.invalid("REQUIRED_DISPLAY_MISSING", "Mẫu thiếu trường hiển thị: " + String.join(", ", missing));
        }
        return bindings;
    }

    private void validateBlock(JsonNode block, JsonNode layout, Set<String> ids, Set<String> bindings, String group,
                               int depth, boolean conditional, InvoiceCatalogSnapshot definitions) {
        if (depth > 16 || ids.size() >= 500) fail("Bố cục vượt giới hạn độ sâu hoặc số khối");
        properties(block, Set.of("id", "type", "children", "style", "headerStyle", "cellStyle", "condition", "text", "fieldKey", "formatter", "label", "asset", "widthMm", "heightMm", "gapMm", "ratios", "columns", "dataSource", "repeatHeader", "rowSplitPolicy"));
        String id = block.path("id").asText();
        if (!id.matches("[a-zA-Z][a-zA-Z0-9_-]{0,63}") || !ids.add(id)) fail("ID khối thiếu, trùng hoặc không hợp lệ");
        String type = block.path("type").asText();
        if (!TYPES.contains(type)) fail("Loại khối không được hỗ trợ: " + type);
        validateStyle(block.path("style"));
        number(block, "gapMm", 0, 20, 2);
        number(block, "widthMm", 1, 270, 30);
        number(block, "heightMm", 1, 150, 10);
        boolean hasCondition = conditional || block.has("condition");
        if (block.has("condition")) validateCondition(block.get("condition"), 0, definitions);
        if (type.equals("field")) {
            String key = block.path("fieldKey").asText();
            validateField(key, group, definitions);
            if (!hasCondition) bindings.add(key);
            validateFormatter(block);
        }
        if (type.equals("text") && (!block.path("text").isTextual() || block.path("text").asText().length() > 5000)) fail("Nội dung text không hợp lệ");
        if (type.equals("image") && !layout.path("assets").has(block.path("asset").asText())) fail("Ảnh chưa có trong manifest");
        if (type.endsWith("Table") || type.equals("table")) {
            validateStyle(block.path("headerStyle"));
            validateStyle(block.path("cellStyle"));
            String expected = type.equals("table") ? block.path("dataSource").asText() : switch (type) { case "itemsTable" -> "items"; case "feesTable" -> "fees"; default -> "taxGroups"; };
            if (!block.path("dataSource").asText(expected).equals(expected)) fail("Nguồn bảng không khớp loại bảng");
            var dataset = definitions.requireDataset(expected);
            if (!"MANY".equals(dataset.cardinality()) || dataset.parent() == null
                    || !"document".equals(definitions.rowScope(dataset.parent())))
                fail("Bảng chỉ hỗ trợ danh sách lặp ở cấp hóa đơn");
            JsonNode columns = block.path("columns");
            if (!columns.isArray() || columns.isEmpty() || columns.size() > 15) fail("Số cột bảng không hợp lệ");
            double total = 0;
            Set<String> columnIds = new HashSet<>();
            for (JsonNode column : columns) {
                properties(column, Set.of("id", "label", "fieldKey", "widthPercent", "formatter", "style"));
                validateStyle(column.path("style"));
                String columnId = column.path("id").asText();
                if (!columnId.matches("[a-zA-Z][a-zA-Z0-9_-]{0,63}") || !columnIds.add(columnId)) fail("ID cột không hợp lệ");
                validateField(column.path("fieldKey").asText(), expected, definitions);
                validateFormatter(column);
                total += number(column, "widthPercent", 1, 100, 0);
                if (!hasCondition) bindings.add(column.path("fieldKey").asText());
            }
            if (Math.abs(total - 100) > 0.01) fail("Tổng độ rộng cột phải bằng 100%");
            choice(block, "rowSplitPolicy", Set.of("splitIfOversized"), "splitIfOversized");
        }
        JsonNode children = block.path("children");
        if (Set.of("stack", "columns", "totals", "relatedInvoice").contains(type)) {
            if (!children.isArray() || children.size() > 100) fail("Danh sách khối con không hợp lệ");
            for (JsonNode child : children) validateBlock(child, layout, ids, bindings, group, depth + 1, hasCondition, definitions);
            if (type.equals("columns") && children.size() > 6) fail("Tối đa 6 cột trong một vùng");
        } else if (block.has("children")) fail("Loại khối không nhận children");
    }

    private void validateField(String key, String group, InvoiceCatalogSnapshot definitions) {
        var field = definitions.requireField(key);
        String scope = definitions.rowScope(field.dataset());
        if (!"document".equals(scope) && !scope.equals(group)) fail("Trường lặp phải nằm trong bảng đúng nhóm: " + key);
        if (!group.isEmpty() && !scope.equals(group)) fail("Binding cột không thuộc nhóm " + group);
    }

    private void validateCondition(JsonNode condition, int depth, InvoiceCatalogSnapshot definitions) {
        if (depth > 6) fail("Điều kiện quá sâu");
        properties(condition, Set.of("op", "fieldKey", "value", "values", "conditions", "condition"));
        String op = condition.path("op").asText();
        if (Set.of("hasValue", "equals", "in").contains(op)) {
            validateField(condition.path("fieldKey").asText(), "", definitions);
            if (op.equals("equals") && !condition.path("value").isValueNode()) fail("Giá trị điều kiện không hợp lệ");
            if (op.equals("in") && (!condition.path("values").isArray() || condition.path("values").size() > 30)) fail("Danh sách điều kiện không hợp lệ");
        } else if (Set.of("all", "any").contains(op)) {
            if (!condition.path("conditions").isArray() || condition.path("conditions").size() > 10) fail("Điều kiện không hợp lệ");
            for (JsonNode child : condition.path("conditions")) validateCondition(child, depth + 1, definitions);
        } else if (op.equals("not")) validateCondition(condition.path("condition"), depth + 1, definitions);
        else fail("Phép so sánh không được hỗ trợ");
    }

    private void validateStyle(JsonNode style) {
        if (style.isMissingNode()) return;
        properties(style, Set.of("fontSizePt", "fontWeight", "fontStyle", "textAlign", "color", "backgroundColor", "paddingMm", "borderWidthMm", "borderColor", "wrap"));
        number(style, "fontSizePt", 7, 36, 10);
        number(style, "paddingMm", 0, 10, 0);
        number(style, "borderWidthMm", 0, 2, 0);
        choice(style, "fontWeight", Set.of("400", "700"), "400");
        choice(style, "fontStyle", Set.of("normal", "italic"), "normal");
        choice(style, "textAlign", Set.of("left", "center", "right"), "left");
        choice(style, "wrap", Set.of("expand"), "expand");
        for (String key : List.of("color", "backgroundColor", "borderColor")) color(style, key);
    }

    public String compile(JsonNode layout, Map<String, Object> data) {
        validate(layout, false);
        JsonNode page = layout.path("page");
        JsonNode theme = layout.path("theme");
        String margin = String.join(" ", List.of("top", "right", "bottom", "left").stream().map(s -> page.path("marginMm").path(s).asText("10") + "mm").toList());
        String family = theme.path("fontAssetFamily").asText("body");
        String css = "@page{size:" + page.path("format").asText("A4") + " " + page.path("orientation").asText("portrait") + ";margin:" + margin + "}"
                + "*{box-sizing:border-box}html,body{margin:0;padding:0}body{font-family:'invoice-" + family + "';font-size:" + theme.path("fontSizePt").asText("10") + "pt;line-height:" + theme.path("lineHeight").asText("1.3") + ";color:" + theme.path("color").asText("#000000") + "}"
                + ".block{overflow-wrap:anywhere;min-width:0}.text{white-space:pre-wrap}.stack,.totals{display:flex;flex-direction:column}.columns{display:flex;align-items:stretch}.columns>.block{flex:1}table{width:100%;border-collapse:collapse;table-layout:fixed}th,td{padding:1.5mm;border:.2mm solid #cbd5e1;vertical-align:top;overflow-wrap:anywhere}thead{display:table-header-group}tr{break-inside:avoid}tr.oversized{break-inside:auto}.totals,.signature{break-inside:avoid}.divider hr{margin:0;border:0;border-top:.25mm solid currentColor}.page-break{break-before:page}img{max-width:100%;object-fit:contain}p{margin:0}.sample{color:#64748b;text-align:center;font-size:8pt;margin-bottom:3mm}";
        boolean landscape = "landscape".equals(page.path("orientation").asText());
        boolean a5 = "A5".equals(page.path("format").asText());
        double shortSide = a5 ? 148 : 210, longSide = a5 ? 210 : 297;
        double width = (landscape ? longSide : shortSide) - page.path("marginMm").path("left").asDouble(10) - page.path("marginMm").path("right").asDouble(10);
        double height = (landscape ? shortSide : longSide) - page.path("marginMm").path("top").asDouble(10) - page.path("marginMm").path("bottom").asDouble(10);
        StringBuilder out = new StringBuilder("<!doctype html><html><head><meta charset=\"UTF-8\"><meta name=\"invoice-printable-width-mm\" content=\"" + width + "\"><meta name=\"invoice-printable-height-mm\" content=\"" + height + "\"><style>" + css + "</style></head><body>");
        if (!String.valueOf(data.getOrDefault("presentation.sampleMarker", "")).isBlank()) out.append("<div class=\"sample\">Mẫu — Dữ liệu minh họa</div>");
        renderBlock(layout.path("body"), layout, data, out);
        return out.append("</body></html>").toString();
    }

    private void renderBlock(JsonNode block, JsonNode layout, Map<String, Object> data, StringBuilder out) {
        if (block.has("condition") && !matches(block.get("condition"), data)) return;
        String type = block.path("type").asText();
        out.append("<div class=\"block ").append(type).append("\" data-block-id=\"").append(escape(block.path("id").asText())).append("\" style=\"").append(style(block.path("style")));
        if (Set.of("stack", "columns", "totals").contains(type)) out.append("gap:").append(block.path("gapMm").asText("2")).append("mm;");
        out.append("\">");
        switch (type) {
            case "stack", "columns", "totals", "relatedInvoice" -> { for (JsonNode child : block.path("children")) renderBlock(child, layout, data, out); }
            case "text" -> out.append(escape(block.path("text").asText()));
            case "field" -> {
                if (!block.path("label").asText().isBlank()) out.append("<span>").append(escape(block.path("label").asText())).append(": </span>");
                out.append(escape(format(data.get(block.path("fieldKey").asText()), block.path("formatter").asText("plainText"))));
            }
            case "image" -> out.append("<img alt=\"\" src=\"https://invoice-assets.invalid/").append(layout.path("assets").path(block.path("asset").asText()).path("assetId").asLong()).append("\" style=\"width:").append(block.path("widthMm").asText("30")).append("mm;height:").append(block.path("heightMm").asText("15")).append("mm\">");
            case "itemsTable", "taxSummaryTable", "feesTable", "table" -> renderTable(block, data, out);
            case "divider" -> out.append("<hr>");
            case "spacer" -> out.append("<div style=\"height:").append(block.path("heightMm").asText("5")).append("mm\"></div>");
            case "pageBreak" -> out.append("<div class=\"page-break\"></div>");
            case "signatureSummary" -> out.append("<div class=\"signature\">").append(escape(String.valueOf(data.getOrDefault("presentation.signatureSummary", "")))).append("</div>");
            default -> throw new IllegalStateException(type);
        }
        out.append("</div>");
    }

    @SuppressWarnings("unchecked")
    private void renderTable(JsonNode block, Map<String, Object> data, StringBuilder out) {
        String group = block.path("type").asText().equals("table") ? block.path("dataSource").asText() : switch (block.path("type").asText()) { case "itemsTable" -> "items"; case "feesTable" -> "fees"; default -> "taxGroups"; };
        out.append("<table><colgroup>");
        for (JsonNode column : block.path("columns")) out.append("<col style=\"width:").append(column.path("widthPercent").asText()).append("%\">");
        out.append("</colgroup><thead><tr>");
        for (JsonNode column : block.path("columns")) out.append("<th style=\"").append(style(block.path("headerStyle"))).append("\">").append(escape(column.path("label").asText())).append("</th>");
        out.append("</tr></thead><tbody>");
        Object value = data.get(group);
        if (value instanceof List<?> rows) {
            for (Object row : rows) {
                Map<String, Object> values = (Map<String, Object>) row;
                out.append("<tr>");
                for (JsonNode column : block.path("columns")) out.append("<td style=\"").append(style(block.path("cellStyle"))).append(style(column.path("style"))).append("\">").append(escape(format(values.get(column.path("fieldKey").asText()), column.path("formatter").asText("plainText")))).append("</td>");
                out.append("</tr>");
            }
        }
        out.append("</tbody></table>");
    }

    private boolean matches(JsonNode node, Map<String, Object> data) {
        String value = String.valueOf(data.getOrDefault(node.path("fieldKey").asText(), ""));
        return switch (node.path("op").asText()) {
            case "hasValue" -> !value.isBlank();
            case "equals" -> value.equals(node.path("value").asText());
            case "in" -> { boolean found = false; for (JsonNode v : node.path("values")) found |= value.equals(v.asText()); yield found; }
            case "all" -> { boolean yes = true; for (JsonNode c : node.path("conditions")) yes &= matches(c, data); yield yes; }
            case "any" -> { boolean yes = false; for (JsonNode c : node.path("conditions")) yes |= matches(c, data); yield yes; }
            case "not" -> !matches(node.path("condition"), data);
            default -> false;
        };
    }

    private static String style(JsonNode node) {
        StringBuilder css = new StringBuilder();
        Map<String, String> names = Map.of("fontSizePt", "font-size", "fontWeight", "font-weight", "fontStyle", "font-style", "textAlign", "text-align", "color", "color", "backgroundColor", "background-color", "paddingMm", "padding", "borderColor", "border-color");
        names.forEach((key, name) -> { if (node.has(key)) css.append(name).append(':').append(node.get(key).asText()).append(key.endsWith("Pt") ? "pt" : key.endsWith("Mm") ? "mm" : "").append(';'); });
        if (node.path("borderWidthMm").asDouble() > 0) css.append("border-style:solid;border-width:").append(node.get("borderWidthMm").asText()).append("mm;");
        return css.toString();
    }

    static String format(Object value, String formatter) {
        String text = value == null ? "" : String.valueOf(value);
        if (text.isBlank()) return "";
        try {
            if (formatter.equals("moneyByCurrency") || formatter.equals("decimal")) {
                var symbols = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("vi-VN"));
                return new DecimalFormat("#,##0.####################", symbols).format(new BigDecimal(text));
            }
            if (formatter.equals("date")) return LocalDate.parse(text).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        } catch (RuntimeException ex) { throw TemplateException.invalid("INVALID_FIELD_VALUE", "Dữ liệu không đúng định dạng " + formatter); }
        return text;
    }

    private static void validateFormatter(JsonNode node) { if (!FORMATTERS.contains(node.path("formatter").asText("plainText"))) fail("Formatter không được hỗ trợ"); }
    private static void properties(JsonNode node, Set<String> allowed) {
        if (!node.isObject()) fail("Cấu trúc layout phải là object");
        node.fieldNames().forEachRemaining(name -> { if (!allowed.contains(name)) fail("Thuộc tính không được hỗ trợ: " + name); });
    }
    private static double number(JsonNode node, String key, double min, double max, double fallback) {
        if (!node.has(key)) return fallback;
        JsonNode value = node.get(key);
        if (!value.isNumber() || !Double.isFinite(value.asDouble()) || value.asDouble() < min || value.asDouble() > max) fail("Giá trị " + key + " ngoài giới hạn");
        return value.asDouble();
    }
    private static void choice(JsonNode node, String key, Set<String> values, String fallback) { if (!values.contains(node.path(key).asText(fallback))) fail("Giá trị " + key + " không hợp lệ"); }
    private static void color(JsonNode node, String key) { if (node.has(key) && !node.get(key).asText().matches("#[0-9a-fA-F]{6}")) fail("Màu không hợp lệ: " + key); }
    private static void fail(String message) { throw TemplateException.invalid("INVALID_LAYOUT", message); }
    public static String escape(String text) { return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"); }
}
