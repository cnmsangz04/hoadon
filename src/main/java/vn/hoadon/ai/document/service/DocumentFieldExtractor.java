package vn.hoadon.ai.document.service;

import org.springframework.stereotype.Component;
import vn.hoadon.ai.document.controller.DocumentRecognitionDtos;
import vn.hoadon.ai.ocr.config.OcrProperties;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DocumentFieldExtractor {
    private static final Pattern TAX_CODE = Pattern.compile("(?<!\\d)(\\d{10}(?:-?\\d{3})?|\\d{12})(?!\\d)");
    private static final Pattern DATE = Pattern.compile("(?<!\\d)(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})(?!\\d)");
    private static final Pattern ISO_DATE = Pattern.compile("(?<!\\d)(\\d{4})-(\\d{1,2})-(\\d{1,2})(?!\\d)");
    private static final Pattern WORD_DATE = Pattern.compile("(?iu)ngày\\s+(\\d{1,2})\\s+tháng\\s+(\\d{1,2})\\s+năm\\s+(\\d{4})");
    private static final Pattern MONEY = Pattern.compile("(?<![\\d.,])(\\d{1,3}(?:[.,\\s]\\d{3})+(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)(?![\\d.,])");
    private static final Pattern ITEM_LINE = Pattern.compile(
            "(?iu)^(?:\\d{1,3}[.)]?\\s+)?(.{3,100}?)\\s+(cái|cai|chiếc|chiec|bộ|bo|hộp|hop|kg|g|tấn|tan|m|m2|m²|m3|m³|lít|lit|chai|thùng|thung|dịch vụ|dich vu|lần|lan|giờ|gio|ngày|ngay|tháng|thang)\\s+([\\d.,]+)\\s+([\\d.,]+)(?:\\s+(\\d{1,2}%|kct|kkknt))?\\s+([\\d.,]+)$");

    private final OcrProperties properties;

    public DocumentFieldExtractor(OcrProperties properties) {
        this.properties = properties;
    }

    public ParsedDocument extract(String text, double sourceConfidence) {
        List<String> lines = cleanLines(text);
        double base = clamp(sourceConfidence <= 0d ? 0.5d : sourceConfidence);
        ParsedField sellerName = labeled(lines, List.of("ten nguoi ban", "don vi ban hang", "nguoi ban", "don vi xuat hang"), base);
        ParsedField buyerName = labeled(lines, List.of("ten nguoi mua", "don vi mua hang", "nguoi mua", "khach hang", "don vi nhan", "dai ly nhan"), base);
        ParsedField sellerTax = labeledTax(lines, List.of("nguoi ban", "don vi ban", "don vi xuat", "seller"), base);
        ParsedField buyerTax = labeledTax(lines, List.of("nguoi mua", "don vi mua", "don vi nhan", "dai ly nhan", "buyer", "khach hang"), base);
        ParsedField sellerAddress = labeled(lines, List.of("dia chi nguoi ban", "dia chi don vi ban", "dia chi don vi xuat"), base);
        ParsedField buyerAddress = labeled(lines, List.of("dia chi nguoi mua", "dia chi don vi mua", "dia chi khach hang", "dia chi don vi nhan", "dia chi dai ly"), base);
        if (sellerAddress.empty() || buyerAddress.empty()) {
            List<ParsedField> addresses = allLabeled(lines, List.of("dia chi"), base);
            if (sellerAddress.empty() && !addresses.isEmpty()) sellerAddress = addresses.get(0);
            if (buyerAddress.empty() && addresses.size() > 1) buyerAddress = addresses.get(1);
        }
        List<ParsedField> taxes = allTaxCodes(lines, base);
        if (sellerTax.empty() && !taxes.isEmpty()) sellerTax = taxes.get(0);
        if (buyerTax.empty() && taxes.size() > 1) buyerTax = taxes.get(1);

        ParsedField date = findDate(lines, base);
        ParsedField subtotal = labeledMoney(lines, List.of("cong tien hang", "tien hang", "subtotal"), base);
        ParsedField vat = labeledMoney(lines, List.of("tien thue gtgt", "tien thue", "thue gtgt"), base);
        ParsedField total = labeledMoney(lines, List.of("tong cong thanh toan", "tong tien thanh toan", "tong thanh toan", "total"), base);
        ParsedField currency = currency(lines, base);
        List<ParsedItem> items = items(lines, base);
        return new ParsedDocument(sellerName, sellerTax, sellerAddress, buyerName, buyerTax, buyerAddress,
                date, currency, items, subtotal, vat, total);
    }

    public DocumentRecognitionDtos.FieldValue view(ParsedField field) {
        double confidence = field == null ? 0d : clamp(field.confidence());
        String value = field == null ? "" : safe(field.value());
        return new DocumentRecognitionDtos.FieldValue(value, round(confidence),
                value.isBlank() || confidence < properties.getLowConfidenceThreshold(),
                field == null ? "" : safe(field.evidence()));
    }

    private ParsedField labeled(List<String> lines, List<String> labels, double base) {
        for (String line : lines) {
            String normalized = normalize(line);
            for (String label : labels) {
                if (normalized.startsWith("dia chi") && !label.startsWith("dia chi")) continue;
                int position = normalized.indexOf(label);
                if (position < 0) continue;
                String value = afterLabel(line);
                if (!value.isBlank() && !normalize(value).equals(label)) {
                    return field(value, base * 0.92d, line);
                }
            }
        }
        return ParsedField.emptyField();
    }

    private List<ParsedField> allLabeled(List<String> lines, List<String> labels, double base) {
        List<ParsedField> result = new ArrayList<>();
        for (String line : lines) {
            String normalized = normalize(line);
            if (labels.stream().noneMatch(normalized::contains)) continue;
            String value = afterLabel(line);
            if (!value.isBlank()) result.add(field(value, base * 0.88d, line));
        }
        return result;
    }

    private ParsedField labeledTax(List<String> lines, List<String> parties, double base) {
        for (String line : lines) {
            String normalized = normalize(line);
            if (!normalized.contains("ma so thue") && !normalized.contains("mst")) continue;
            if (parties.stream().noneMatch(normalized::contains)) continue;
            Matcher matcher = TAX_CODE.matcher(line.replace(" ", ""));
            if (matcher.find()) return field(normalizeTaxCode(matcher.group(1)), base * 0.98d, line);
        }
        return ParsedField.emptyField();
    }

    private List<ParsedField> allTaxCodes(List<String> lines, double base) {
        List<ParsedField> result = new ArrayList<>();
        for (String line : lines) {
            String compact = line.replace(" ", "");
            Matcher matcher = TAX_CODE.matcher(compact);
            while (matcher.find()) result.add(field(normalizeTaxCode(matcher.group(1)), base * 0.9d, line));
        }
        return result.stream().distinct().toList();
    }

    private ParsedField findDate(List<String> lines, double base) {
        for (String line : lines) {
            Matcher words = WORD_DATE.matcher(line);
            if (words.find()) return dateField(words.group(1), words.group(2), words.group(3), base, line);
            Matcher iso = ISO_DATE.matcher(line);
            if (iso.find()) return dateField(iso.group(3), iso.group(2), iso.group(1), base, line);
            Matcher date = DATE.matcher(line);
            if (date.find()) return dateField(date.group(1), date.group(2), date.group(3), base, line);
        }
        return ParsedField.emptyField();
    }

    private ParsedField dateField(String day, String month, String year, double base, String evidence) {
        try {
            LocalDate value = LocalDate.of(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
            return field(value.format(DateTimeFormatter.ISO_LOCAL_DATE), base * 0.96d, evidence);
        } catch (RuntimeException ex) {
            return ParsedField.emptyField();
        }
    }

    private ParsedField labeledMoney(List<String> lines, List<String> labels, double base) {
        for (int index = lines.size() - 1; index >= 0; index--) {
            String line = lines.get(index);
            String normalized = normalize(line);
            if (labels.stream().noneMatch(normalized::contains)) continue;
            List<BigDecimal> values = moneyValues(line);
            if (!values.isEmpty()) return field(values.get(values.size() - 1).stripTrailingZeros().toPlainString(),
                    base * 0.94d, line);
        }
        return ParsedField.emptyField();
    }

    private ParsedField currency(List<String> lines, double base) {
        for (String line : lines) {
            String normalized = normalize(line);
            if (normalized.contains("vnd") || normalized.contains("viet nam dong") || normalized.contains("dong")) {
                return field("VND", base * 0.95d, line);
            }
            if (normalized.matches(".*\\busd\\b.*")) return field("USD", base * 0.95d, line);
        }
        return ParsedField.emptyField();
    }

    private List<ParsedItem> items(List<String> lines, double base) {
        List<ParsedItem> result = new ArrayList<>();
        int maximum = Math.max(1, properties.getMaxItems());
        for (String line : lines) {
            if (result.size() >= maximum) break;
            String normalized = normalize(line);
            if (normalized.contains("tong") || normalized.contains("cong tien") || normalized.contains("tien thue")) continue;
            Matcher matcher = ITEM_LINE.matcher(line);
            if (!matcher.matches()) continue;
            BigDecimal quantity = parseNumber(matcher.group(3));
            BigDecimal unitPrice = parseNumber(matcher.group(4));
            BigDecimal lineTotal = parseNumber(matcher.group(6));
            if (quantity == null || unitPrice == null || lineTotal == null || quantity.signum() <= 0
                    || unitPrice.signum() < 0 || lineTotal.signum() < 0) continue;
            String vat = normalizeVat(matcher.group(5));
            result.add(new ParsedItem(field(matcher.group(1), base * 0.82d, line),
                    field(matcher.group(2), base * 0.88d, line),
                    field(quantity.stripTrailingZeros().toPlainString(), base * 0.9d, line),
                    field(unitPrice.stripTrailingZeros().toPlainString(), base * 0.9d, line),
                    vat.isBlank() ? ParsedField.emptyField() : field(vat, base * 0.86d, line),
                    field(lineTotal.stripTrailingZeros().toPlainString(), base * 0.9d, line)));
        }
        return result;
    }

    private List<BigDecimal> moneyValues(String line) {
        List<BigDecimal> result = new ArrayList<>();
        Matcher matcher = MONEY.matcher(line);
        while (matcher.find()) {
            BigDecimal value = parseNumber(matcher.group(1));
            if (value != null) result.add(value);
        }
        return result;
    }

    private BigDecimal parseNumber(String raw) {
        if (raw == null) return null;
        String value = raw.replaceAll("[^0-9.,-]", "");
        if (value.isBlank()) return null;
        int comma = value.lastIndexOf(',');
        int dot = value.lastIndexOf('.');
        char decimal = 0;
        int position = Math.max(comma, dot);
        if (position >= 0) {
            int decimals = value.length() - position - 1;
            boolean repeated = value.substring(0, position).indexOf(value.charAt(position)) >= 0;
            if (!repeated && decimals > 0 && decimals <= 2) decimal = value.charAt(position);
        }
        String normalized;
        if (decimal == 0) normalized = value.replace(".", "").replace(",", "");
        else {
            String integer = value.substring(0, position).replace(".", "").replace(",", "");
            normalized = integer + "." + value.substring(position + 1);
        }
        try { return new BigDecimal(normalized); }
        catch (NumberFormatException ex) { return null; }
    }

    private String normalizeVat(String value) {
        if (value == null || value.isBlank()) return "";
        String normalized = normalize(value);
        if (normalized.contains("kct") || normalized.contains("kkknt")) return "-1";
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? "" : digits;
    }

    private String normalizeTaxCode(String value) {
        String digits = value.replaceAll("\\D", "");
        return digits.length() == 13 ? digits.substring(0, 10) + "-" + digits.substring(10) : digits;
    }

    private List<String> cleanLines(String text) {
        if (text == null || text.isBlank()) return List.of();
        List<String> lines = new ArrayList<>();
        for (String raw : text.split("\\R")) {
            String value = raw.replaceAll("\\s+", " ").trim();
            if (!value.isBlank() && !value.matches("## Trang \\d+")) lines.add(value);
        }
        return lines;
    }

    private String afterLabel(String line) {
        int colon = Math.max(line.indexOf(':'), line.indexOf('：'));
        if (colon >= 0 && colon + 1 < line.length()) return line.substring(colon + 1).trim();
        int dash = line.indexOf(" - ");
        return dash >= 0 && dash + 3 < line.length() ? line.substring(dash + 3).trim() : "";
    }

    private ParsedField field(String value, double confidence, String evidence) {
        return new ParsedField(value == null ? "" : value.trim(), clamp(confidence), evidence == null ? "" : evidence.trim());
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replace('đ', 'd');
        return normalized.replaceAll("\\s+", " ").trim();
    }

    private double clamp(double value) { return Math.max(0d, Math.min(1d, value)); }
    private double round(double value) { return Math.round(value * 10_000d) / 10_000d; }
    private String safe(String value) { return value == null ? "" : value; }

    public record ParsedField(String value, double confidence, String evidence) {
        static ParsedField emptyField() { return new ParsedField("", 0d, ""); }
        boolean empty() { return value == null || value.isBlank(); }
    }
    public record ParsedItem(ParsedField name, ParsedField unit, ParsedField quantity, ParsedField unitPrice,
                             ParsedField vatRate, ParsedField lineTotal) { }
    public record ParsedDocument(ParsedField sellerName, ParsedField sellerTaxCode, ParsedField sellerAddress,
                                 ParsedField buyerName, ParsedField buyerTaxCode, ParsedField buyerAddress,
                                 ParsedField documentDate, ParsedField currency, List<ParsedItem> items,
                                 ParsedField subtotal, ParsedField vatAmount, ParsedField total) { }
}
