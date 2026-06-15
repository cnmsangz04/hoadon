package vn.hoadon.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Validate signed registration declaration XML before simulating CQT responses.
 */
public final class RegisterInvoiceXmlTaxValidator {
    private RegisterInvoiceXmlTaxValidator() {}

    public static Result validateForReceive(String xml) {
        List<String> errors = new ArrayList<>();
        Document doc = parseTaxXml(xml, errors);
        if (doc == null) return Result.invalid(errors);

        Element tkhai = doc.getDocumentElement();
        if (tkhai == null || !"TKhai".equals(tagName(tkhai))) {
            errors.add("Thiếu thẻ gốc TKhai");
            return Result.invalid(errors);
        }

        Element dlTkhai = requireChild(errors, tkhai, "TKhai", "DLTKhai");
        if (dlTkhai != null) {
            String id = dlTkhai.getAttribute("Id");
            if (!hasText(id)) errors.add("TKhai/DLTKhai@Id: bắt buộc");
        }
        Element dscks = requireChild(errors, tkhai, "TKhai", "DSCKS");
        validateSignature(errors, dscks);
        return errors.isEmpty() ? Result.ok() : Result.invalid(errors);
    }

    public static Result validate(String xml) {
        List<String> errors = new ArrayList<>();
        Document doc = parseTaxXml(xml, errors);
        if (doc == null) return Result.invalid(errors);

        Element tkhai = doc.getDocumentElement();
        if (tkhai == null || !"TKhai".equals(tagName(tkhai))) {
            errors.add("Thiếu thẻ gốc TKhai");
            return Result.invalid(errors);
        }

        Element dlTkhai = requireChild(errors, tkhai, "TKhai", "DLTKhai");
        Element dscks = requireChild(errors, tkhai, "TKhai", "DSCKS");
        if (dlTkhai != null) {
            String id = dlTkhai.getAttribute("Id");
            if (!hasText(id)) errors.add("TKhai/DLTKhai@Id: bắt buộc");

            Element ttChung = requireChild(errors, dlTkhai, "TKhai/DLTKhai", "TTChung");
            Element ndTkhai = requireChild(errors, dlTkhai, "TKhai/DLTKhai", "NDTKhai");
            if (ttChung != null) validateTtChung(errors, ttChung);
            if (ndTkhai != null) validateNdTkhai(errors, ndTkhai);
        }
        validateSignature(errors, dscks);
        return errors.isEmpty() ? Result.ok() : Result.invalid(errors);
    }

    private static Document parseTaxXml(String xml, List<String> errors) {
        if (!hasText(xml)) {
            errors.add("XML tờ khai rỗng");
            return null;
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            setFeatureIfSupported(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
            setFeatureIfSupported(factory, "http://xml.org/sax/features/external-general-entities", false);
            setFeatureIfSupported(factory, "http://xml.org/sax/features/external-parameter-entities", false);
            setFeatureIfSupported(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            factory.setNamespaceAware(false);
            Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
            doc.getDocumentElement().normalize();
            return doc;
        } catch (Exception e) {
            errors.add("XML không đúng định dạng: " + e.getMessage());
            return null;
        }
    }

    private static void setFeatureIfSupported(DocumentBuilderFactory factory, String feature, boolean enabled) {
        try {
            factory.setFeature(feature, enabled);
        } catch (Exception ignore) {
            // Some XML parsers do not expose every hardening feature.
        }
    }

    private static void validateTtChung(List<String> errors, Element ttChung) {
        String path = "TKhai/DLTKhai/TTChung";
        String pban = requireText(errors, ttChung, path, "PBan", 6);
        if (hasText(pban) && !"2.1.0".equals(pban)) {
            errors.add(path + "/PBan: phải có giá trị 2.1.0");
        }
        requireText(errors, ttChung, path, "MSo", 15);
        requireText(errors, ttChung, path, "Ten", 100);
        String hthuc = requireNumber(errors, ttChung, path, "HThuc", 1, 0);
        requireOneOf(errors, path + "/HThuc", hthuc, "1", "2");
        requireText(errors, ttChung, path, "TNNT", 400);
        requireText(errors, ttChung, path, "MST", 14);
        requireText(errors, ttChung, path, "CQTQLy", 100);
        requireText(errors, ttChung, path, "MCQTQLy", 5);
        requireText(errors, ttChung, path, "TNDDPLuat", 50);
        requireText(errors, ttChung, path, "DTDDPLuat", 20);
        requireText(errors, ttChung, path, "CCCDan", 12);
        optionalLength(errors, ttChung, path, "SHChieu", 20);
        requireDate(errors, ttChung, path, "NSDDPLuat");
        String gtinh = requireNumber(errors, ttChung, path, "GTinh", 1, 0);
        requireOneOf(errors, path + "/GTinh", gtinh, "0", "1");
        requireText(errors, ttChung, path, "DCLHe", 400);
        requireText(errors, ttChung, path, "DCTDTu", 50);
        optionalLength(errors, ttChung, path, "NLHe", 50);
        optionalLength(errors, ttChung, path, "DTLHe", 20);
        requireText(errors, ttChung, path, "DDanh", 50);
        requireDate(errors, ttChung, path, "NLap");
    }

    private static void validateNdTkhai(List<String> errors, Element ndTkhai) {
        validateFlagGroup(errors, requireChild(errors, ndTkhai, "TKhai/DLTKhai/NDTKhai", "HTHDon"),
                "TKhai/DLTKhai/NDTKhai/HTHDon", true, "CMa", "KCMa", "CMTMTTien");
        validateFlagGroup(errors, requireChild(errors, ndTkhai, "TKhai/DLTKhai/NDTKhai", "HTGDLHDDT"),
                "TKhai/DLTKhai/NDTKhai/HTGDLHDDT", false,
                "NNTDBKKhan", "NNTKTDNUBND", "CQXLTSCong", "CDLTTDCQT", "CDLQTCTN");
        validateFlagGroup(errors, requireChild(errors, ndTkhai, "TKhai/DLTKhai/NDTKhai", "PThuc"),
                "TKhai/DLTKhai/NDTKhai/PThuc", true, "CDDu", "CBTHop");
        validateFlagGroup(errors, requireChild(errors, ndTkhai, "TKhai/DLTKhai/NDTKhai", "LHDSDung"),
                "TKhai/DLTKhai/NDTKhai/LHDSDung", true,
                "HDGTGT", "HDGTGT_BienLai", "HDBHang", "HDBHang_BienLai", "HDThuongMai",
                "HDBTSCong", "HDBHDTQGia", "HDKhac", "CTu");

        validateCertificates(errors, requireChild(errors, ndTkhai, "TKhai/DLTKhai/NDTKhai", "DSCTSSDung"));
        validateSolutionProviders(errors, requireChild(errors, ndTkhai, "TKhai/DLTKhai/NDTKhai", "TTTCGP"));
        validateTransmitProviders(errors, requireChild(errors, ndTkhai, "TKhai/DLTKhai/NDTKhai", "TTTCTN"));
    }

    private static void validateFlagGroup(List<String> errors, Element group, String path, boolean requireAnySelected, String... tags) {
        if (group == null) return;
        boolean anySelected = false;
        for (String tag : tags) {
            String value = requireNumber(errors, group, path, tag, 1, 0);
            if (hasText(value) && !"0".equals(value) && !"1".equals(value)) {
                errors.add(path + "/" + tag + ": chỉ nhận 0 hoặc 1");
            }
            anySelected = anySelected || "1".equals(value);
        }
        if (requireAnySelected && !anySelected) {
            errors.add(path + ": bắt buộc chọn ít nhất một giá trị");
        }
    }

    private static void validateCertificates(List<String> errors, Element dscts) {
        if (dscts == null) return;
        List<Element> rows = children(dscts, "CTS");
        if (rows.isEmpty()) {
            errors.add("TKhai/DLTKhai/NDTKhai/DSCTSSDung/CTS: bắt buộc có ít nhất 1 chứng thư số");
            return;
        }
        int index = 0;
        for (Element row : rows) {
            index++;
            String path = "TKhai/DLTKhai/NDTKhai/DSCTSSDung/CTS[" + index + "]";
            optionalNumber(errors, row, path, "STT", 3, 0);
            requireText(errors, row, path, "TTChuc", 400);
            requireText(errors, row, path, "Seri", 40);
            requireDateTime(errors, row, path, "TNgay");
            requireDateTime(errors, row, path, "DNgay");
            String hthuc = requireNumber(errors, row, path, "HThuc", 1, 0);
            requireOneOf(errors, path + "/HThuc", hthuc, "1", "2", "3");
        }
    }

    private static void validateSolutionProviders(List<String> errors, Element group) {
        if (group == null) return;
        List<Element> rows = children(group, "TCGP");
        if (rows.isEmpty()) {
            errors.add("TKhai/DLTKhai/NDTKhai/TTTCGP/TCGP: bắt buộc có thẻ theo XML mẫu");
            return;
        }
        int index = 0;
        for (Element row : rows) {
            index++;
            String path = "TKhai/DLTKhai/NDTKhai/TTTCGP/TCGP[" + index + "]";
            optionalNumber(errors, row, path, "STT", 3, 0);
            requireTagThenOptionalLength(errors, row, path, "TTCGP", 400);
            requireTagThenOptionalLength(errors, row, path, "MSTTCGP", 14);
            requireTagThenOptionalDate(errors, row, path, "TNgay");
            optionalDate(errors, row, path, "DNgay");
            optionalLength(errors, row, path, "GChu", 255);
        }
    }

    private static void validateTransmitProviders(List<String> errors, Element group) {
        if (group == null) return;
        List<Element> rows = children(group, "TCTN");
        if (rows.isEmpty()) {
            errors.add("TKhai/DLTKhai/NDTKhai/TTTCTN/TCTN: bắt buộc có thẻ theo XML mẫu");
            return;
        }
        int index = 0;
        for (Element row : rows) {
            index++;
            String path = "TKhai/DLTKhai/NDTKhai/TTTCTN/TCTN[" + index + "]";
            optionalNumber(errors, row, path, "STT", 3, 0);
            requireTagThenOptionalLength(errors, row, path, "TTCTN", 400);
            requireTagThenOptionalLength(errors, row, path, "MSTTCTN", 14);
            requireTagThenOptionalDate(errors, row, path, "TNgay");
            optionalDate(errors, row, path, "DNgay");
            optionalLength(errors, row, path, "GChu", 255);
        }
    }

    private static void validateSignature(List<String> errors, Element dscks) {
        if (dscks == null) return;
        Element nnt = requireChild(errors, dscks, "TKhai/DSCKS", "NNT");
        Element signature = requireChild(errors, nnt, "TKhai/DSCKS/NNT", "Signature");
        if (signature == null) return;
        requireText(errors, signature, "TKhai/DSCKS/NNT/Signature", "SignerName", 400);
        requireText(errors, signature, "TKhai/DSCKS/NNT/Signature", "SignerTaxCode", 14);
        requireDateTime(errors, signature, "TKhai/DSCKS/NNT/Signature", "SignedAt");
        requireText(errors, signature, "TKhai/DSCKS/NNT/Signature", "SignatureValue", null);
    }

    private static Element requireChild(List<String> errors, Element parent, String path, String tag) {
        if (parent == null) return null;
        Element child = child(parent, tag);
        if (child == null) {
            errors.add(path + "/" + tag + ": bắt buộc");
        }
        return child;
    }

    private static String requireText(List<String> errors, Element parent, String path, String tag, Integer maxLen) {
        Element child = requireChild(errors, parent, path, tag);
        String value = text(child);
        if (!hasText(value)) {
            errors.add(path + "/" + tag + ": bắt buộc");
        } else if (maxLen != null && value.length() > maxLen) {
            errors.add(path + "/" + tag + ": vượt quá " + maxLen + " ký tự");
        }
        return value;
    }

    private static String requireTagThenOptionalLength(List<String> errors, Element parent, String path, String tag, int maxLen) {
        Element child = requireChild(errors, parent, path, tag);
        String value = text(child);
        if (hasText(value) && value.length() > maxLen) {
            errors.add(path + "/" + tag + ": vượt quá " + maxLen + " ký tự");
        }
        return value;
    }

    private static String optionalLength(List<String> errors, Element parent, String path, String tag, int maxLen) {
        Element child = child(parent, tag);
        String value = text(child);
        if (hasText(value) && value.length() > maxLen) {
            errors.add(path + "/" + tag + ": vượt quá " + maxLen + " ký tự");
        }
        return value;
    }

    private static String requireNumber(List<String> errors, Element parent, String path, String tag, int integerDigits, int fractionDigits) {
        String value = requireText(errors, parent, path, tag, null);
        validateNumber(errors, path + "/" + tag, value, integerDigits, fractionDigits);
        return value;
    }

    private static String optionalNumber(List<String> errors, Element parent, String path, String tag, int integerDigits, int fractionDigits) {
        Element child = child(parent, tag);
        String value = text(child);
        if (hasText(value)) {
            validateNumber(errors, path + "/" + tag, value, integerDigits, fractionDigits);
        }
        return value;
    }

    private static String requireDate(List<String> errors, Element parent, String path, String tag) {
        String value = requireText(errors, parent, path, tag, null);
        validateDate(errors, path + "/" + tag, value);
        return value;
    }

    private static String requireDateTime(List<String> errors, Element parent, String path, String tag) {
        String value = requireText(errors, parent, path, tag, null);
        validateDateTime(errors, path + "/" + tag, value);
        return value;
    }

    private static String requireTagThenOptionalDate(List<String> errors, Element parent, String path, String tag) {
        Element child = requireChild(errors, parent, path, tag);
        String value = text(child);
        if (hasText(value)) {
            validateDate(errors, path + "/" + tag, value);
        }
        return value;
    }

    private static String optionalDate(List<String> errors, Element parent, String path, String tag) {
        Element child = child(parent, tag);
        String value = text(child);
        if (hasText(value)) {
            validateDate(errors, path + "/" + tag, value);
        }
        return value;
    }

    private static void requireOneOf(List<String> errors, String path, String value, String... allowed) {
        if (!hasText(value)) return;
        for (String item : allowed) {
            if (item.equals(value)) return;
        }
        errors.add(path + ": giá trị không hợp lệ");
    }

    private static void validateNumber(List<String> errors, String path, String value, int integerDigits, int fractionDigits) {
        if (!hasText(value)) return;
        String normalized = value.trim();
        if (!normalized.matches("[+-]?\\d+(\\.\\d+)?")) {
            errors.add(path + ": phải là số");
            return;
        }
        try {
            new BigDecimal(normalized);
        } catch (Exception e) {
            errors.add(path + ": phải là số");
            return;
        }
        String unsigned = normalized.startsWith("-") || normalized.startsWith("+") ? normalized.substring(1) : normalized;
        String[] parts = unsigned.split("\\.", -1);
        String integerPart = parts[0].replaceFirst("^0+(?!$)", "");
        String fractionPart = parts.length > 1 ? parts[1] : "";
        if (integerDigits >= 0 && integerPart.length() > integerDigits) {
            errors.add(path + ": phần nguyên vượt quá " + integerDigits + " chữ số");
        }
        if (fractionDigits >= 0 && fractionPart.length() > fractionDigits) {
            errors.add(path + ": phần thập phân vượt quá " + fractionDigits + " chữ số");
        }
    }

    private static void validateDate(List<String> errors, String path, String value) {
        if (!hasText(value)) return;
        try {
            LocalDate.parse(value.trim());
        } catch (Exception e) {
            errors.add(path + ": phải là ngày hợp lệ theo định dạng yyyy-MM-dd");
        }
    }

    private static void validateDateTime(List<String> errors, String path, String value) {
        if (!hasText(value)) return;
        String normalized = value.trim();
        try {
            LocalDateTime.parse(normalized);
            return;
        } catch (Exception ignore) {}
        try {
            LocalDate.parse(normalized);
            return;
        } catch (Exception ignore) {}
        errors.add(path + ": phải là ngày/giờ hợp lệ");
    }

    private static Element child(Element parent, String tag) {
        if (parent == null) return null;
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element && tag.equals(tagName(node))) {
                return (Element) node;
            }
        }
        return null;
    }

    private static List<Element> children(Element parent, String tag) {
        List<Element> out = new ArrayList<>();
        if (parent == null) return out;
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element && tag.equals(tagName(node))) {
                out.add((Element) node);
            }
        }
        return out;
    }

    private static String text(Element element) {
        return element == null || element.getTextContent() == null ? "" : element.getTextContent().trim();
    }

    private static String tagName(Node node) {
        if (node == null) return "";
        String localName = node.getLocalName();
        if (localName != null && !localName.isBlank()) return localName;
        String nodeName = node.getNodeName();
        if (nodeName == null) return "";
        int colon = nodeName.indexOf(':');
        return colon >= 0 ? nodeName.substring(colon + 1) : nodeName;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static final class Result {
        private final boolean valid;
        private final String message;

        private Result(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }

        private static Result ok() {
            return new Result(true, "XML tờ khai hợp lệ");
        }

        private static Result invalid(List<String> errors) {
            List<String> safeErrors = errors == null ? Collections.emptyList() : errors;
            if (safeErrors.isEmpty()) {
                return new Result(false, "XML tờ khai không hợp lệ");
            }
            int limit = Math.min(5, safeErrors.size());
            String message = "XML tờ khai không hợp lệ: " + String.join("; ", safeErrors.subList(0, limit));
            if (safeErrors.size() > limit) {
                message += "; ...";
            }
            return new Result(false, message);
        }
    }
}
