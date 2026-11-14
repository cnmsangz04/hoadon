package vn.hoadon.invoice.template.catalog;

import org.springframework.stereotype.Component;

import vn.hoadon.entity.InvoiceEntity;

import java.util.*;
import java.util.function.Function;

@Component
public class InvoiceExternalFieldRegistry {
    public record ContextField(String key, String label, String valueType) {}

    private record Definition(ContextField field, Function<InvoiceEntity, Object> reader) {}

    private static final Map<String, Definition> FIELDS;
    private static final Map<String, String> PRESENTATION =
            Map.of(
                    "document.sourceStage",
                    "sourceStage",
                    "document.statusLabel",
                    "statusLabel",
                    "document.signatureSummary",
                    "signatureSummary",
                    "document.sampleMarker",
                    "sampleMarker",
                    "document.authorityCode",
                    "authorityCode",
                    "document.lookupCode",
                    "lookupCode");

    static {
        Map<String, Definition> fields = new LinkedHashMap<>();
        register(fields, "code", "Mã hóa đơn", "STRING", InvoiceEntity::getCode);
        register(fields, "code_cqt", "Mã của cơ quan thuế", "STRING", InvoiceEntity::getCodeCqt);
        register(
                fields,
                "lookup_code",
                "Mã tra cứu hóa đơn",
                "STRING",
                InvoiceEntity::getLookupCode);
        register(fields, "status", "Trạng thái hóa đơn", "INTEGER", InvoiceEntity::getStatus);
        register(fields, "currency", "Đơn vị tiền tệ", "STRING", InvoiceEntity::getCurrency);
        register(fields, "exchange_rate", "Tỷ giá", "DECIMAL", InvoiceEntity::getExchangeRate);
        register(fields, "total", "Tổng tiền hàng", "DECIMAL", InvoiceEntity::getTotal);
        register(fields, "amount", "Tổng thanh toán", "DECIMAL", InvoiceEntity::getAmount);
        register(fields, "vat_amount", "Tiền thuế", "DECIMAL", InvoiceEntity::getVatAmount);
        register(
                fields,
                "amount_in_words",
                "Số tiền bằng chữ",
                "STRING",
                InvoiceEntity::getAmountInWords);
        FIELDS = Collections.unmodifiableMap(fields);
    }

    private static void register(
            Map<String, Definition> target,
            String column,
            String label,
            String type,
            Function<InvoiceEntity, Object> reader) {
        String key = "invoice." + column;
        target.put(key, new Definition(new ContextField(key, label, type), reader));
    }

    public List<ContextField> available() {
        var values = new ArrayList<>(FIELDS.values().stream().map(Definition::field).toList());
        Map<String, String> labels =
                Map.of(
                        "sourceStage",
                        "Giai đoạn dữ liệu",
                        "statusLabel",
                        "Trạng thái hiển thị",
                        "signatureSummary",
                        "Thông tin chữ ký",
                        "sampleMarker",
                        "Dấu mẫu minh họa",
                        "authorityCode",
                        "Mã cơ quan thuế trên bản in",
                        "lookupCode",
                        "Mã tra cứu trên bản in");
        PRESENTATION.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                        entry ->
                                values.add(
                                        new ContextField(
                                                entry.getKey(),
                                                labels.get(entry.getValue()),
                                                "STRING")));
        return List.copyOf(values);
    }

    public static boolean contextSupports(String key) {
        return FIELDS.containsKey(key) || PRESENTATION.containsKey(key);
    }

    private String value(InvoiceEntity invoice, String path) {
        var definition = FIELDS.get(path);
        if (definition == null)
            throw new IllegalArgumentException("Nguồn dữ liệu chưa được cung cấp: " + path);
        return Objects.toString(definition.reader().apply(invoice), "");
    }

    public Map<String, String> capture(InvoiceEntity invoice, InvoiceCatalogSnapshot snapshot) {
        Map<String, String> context = new TreeMap<>();
        for (var field : snapshot.fields()) {
            if ("CONTEXT".equals(field.sourceType()) && FIELDS.containsKey(field.sourcePath()))
                context.put(field.sourcePath(), value(invoice, field.sourcePath()));
        }
        return context;
    }

    public static String read(Map<String, String> values, String selector) {
        if (values == null) return "";
        if (values.containsKey(selector)) return Objects.toString(values.get(selector), "");
        if (PRESENTATION.containsKey(selector))
            return Objects.toString(values.get(PRESENTATION.get(selector)), "");
        if (selector.startsWith("invoice."))
            return Objects.toString(values.get(selector.substring(8)), "");
        return "";
    }
}
