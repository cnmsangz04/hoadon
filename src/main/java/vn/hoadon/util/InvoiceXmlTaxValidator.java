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
 * Validate signed invoice XML before simulating the tax authority response.
 */
public final class InvoiceXmlTaxValidator {
    private InvoiceXmlTaxValidator() {}

    public static Result validate(String xml) {
        List<String> errors = new ArrayList<>();
        Document doc = parseTaxXml(xml, errors);
        if (doc == null) return Result.invalid(errors);

        Element hdon = doc.getDocumentElement();
        if (hdon == null || !"HDon".equals(tagName(hdon))) {
            errors.add("Thiếu thẻ gốc HDon");
            return Result.invalid(errors);
        }

        Element dlhDon = requireChild(errors, hdon, "HDon", "DLHDon");
        Element ttChung = requireChild(errors, dlhDon, "HDon/DLHDon", "TTChung");
        Element ndhDon = requireChild(errors, dlhDon, "HDon/DLHDon", "NDHDon");
        Element dscks = requireChild(errors, hdon, "HDon", "DSCKS");

        if (ttChung != null) validateTtChung(errors, ttChung);
        if (ndhDon != null) validateNdHDon(errors, ndhDon);
        validateDscks(errors, dscks);

        optionalLength(errors, hdon, "HDon", "MCCQT", 34);
        Element dlQrCode = child(hdon, "DLQRCode");
        if (dlQrCode != null && hasText(text(dlQrCode)) && text(dlQrCode).length() > 512) {
            errors.add("HDon/DLQRCode: vượt quá 512 ký tự");
        }
        return errors.isEmpty() ? Result.ok() : Result.invalid(errors);
    }

    private static Document parseTaxXml(String xml, List<String> errors) {
        if (!hasText(xml)) {
            errors.add("XML hóa đơn rỗng");
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
        String path = "HDon/DLHDon/TTChung";
        String pban = requireText(errors, ttChung, path, "PBan", 6);
        if (hasText(pban) && !"2.1.0".equals(pban)) {
            errors.add(path + "/PBan: phải có giá trị 2.1.0");
        }
        requireText(errors, ttChung, path, "THDon", 100);
        requireText(errors, ttChung, path, "KHMSHDon", 1);
        requireText(errors, ttChung, path, "KHHDon", 6);
        requireNumber(errors, ttChung, path, "SHDon", 8, 0);
        optionalLength(errors, ttChung, path, "MHSo", 20);
        requireDate(errors, ttChung, path, "NLap");
        String hdcttChinh = requireNumber(errors, ttChung, path, "HDCTTChinh", 1, 0);
        if (hasText(hdcttChinh) && !"0".equals(hdcttChinh) && !"1".equals(hdcttChinh)) {
            errors.add(path + "/HDCTTChinh: chỉ nhận 0 hoặc 1");
        }
        optionalLength(errors, ttChung, path, "SBKe", 50);
        optionalDate(errors, ttChung, path, "NBKe");
        String dvtte = requireText(errors, ttChung, path, "DVTTe", 3);
        if (hasText(dvtte) && !"VND".equalsIgnoreCase(dvtte)) {
            requireNumber(errors, ttChung, path, "TGia", 7, 2);
        } else {
            optionalNumber(errors, ttChung, path, "TGia", 7, 2);
        }
        requireText(errors, ttChung, path, "HTTToan", 50);
        requireText(errors, ttChung, path, "MSTTCGP", 14);
        optionalLength(errors, ttChung, path, "MSTDVNUNLHDon", 14);
        optionalLength(errors, ttChung, path, "TDVNUNLHDon", 400);
        optionalLength(errors, ttChung, path, "DCDVNUNLHDon", 400);
        validateRelatedInvoice(errors, child(ttChung, "TTHDLQuan"));
    }

    private static void validateRelatedInvoice(List<String> errors, Element related) {
        if (related == null || !hasAnyElementText(related)) return;
        String path = "HDon/DLHDon/TTChung/TTHDLQuan";
        String tcdon = requireNumber(errors, related, path, "TCHDon", 1, 0);
        if (hasText(tcdon) && !"1".equals(tcdon) && !"2".equals(tcdon)) {
            errors.add(path + "/TCHDon: chỉ nhận 1 hoặc 2");
        }
        requireNumber(errors, related, path, "LHDCLQuan", 1, 0);
        requireText(errors, related, path, "KHMSHDCLQuan", 11);
        requireText(errors, related, path, "KHHDCLQuan", 8);
        requireText(errors, related, path, "SHDCLQuan", 8);
        requireDate(errors, related, path, "NLHDCLQuan");
        optionalLength(errors, related, path, "GChu", 255);
        optionalLength(errors, related, path, "SBKCLQuan", 50);
        optionalDate(errors, related, path, "NBKCLQuan");
    }

    private static void validateNdHDon(List<String> errors, Element ndhDon) {
        Element nban = requireChild(errors, ndhDon, "HDon/DLHDon/NDHDon", "NBan");
        Element nmua = requireChild(errors, ndhDon, "HDon/DLHDon/NDHDon", "NMua");
        Element dshhdvu = requireChild(errors, ndhDon, "HDon/DLHDon/NDHDon", "DSHHDVu");
        Element ttoan = requireChild(errors, ndhDon, "HDon/DLHDon/NDHDon", "TToan");
        if (nban != null) validateSeller(errors, nban);
        if (nmua != null) validateBuyer(errors, nmua);
        if (dshhdvu != null) validateLineItems(errors, dshhdvu);
        if (ttoan != null) validatePayment(errors, ttoan);
    }

    private static void validateSeller(List<String> errors, Element nban) {
        String path = "HDon/DLHDon/NDHDon/NBan";
        requireText(errors, nban, path, "Ten", 400);
        requireText(errors, nban, path, "MST", 14);
        requireText(errors, nban, path, "DChi", 400);
        optionalLength(errors, nban, path, "MCHang", 50);
        optionalLength(errors, nban, path, "TCHang", 400);
        optionalLength(errors, nban, path, "SDThoai", 20);
        optionalLength(errors, nban, path, "DCTDTu", 50);
        optionalLength(errors, nban, path, "STKNHang", 30);
        optionalLength(errors, nban, path, "TNHang", 400);
        optionalLength(errors, nban, path, "Fax", 20);
        optionalLength(errors, nban, path, "Website", 100);
    }

    private static void validateBuyer(List<String> errors, Element nmua) {
        String path = "HDon/DLHDon/NDHDon/NMua";
        optionalLength(errors, nmua, path, "Ten", 400);
        optionalLength(errors, nmua, path, "MST", 14);
        optionalLength(errors, nmua, path, "MDVQHNSach", 7);
        optionalLength(errors, nmua, path, "DChi", 400);
        optionalLength(errors, nmua, path, "MKHang", 50);
        optionalLength(errors, nmua, path, "SDThoai", 20);
        optionalLength(errors, nmua, path, "CCCDan", 12);
        optionalLength(errors, nmua, path, "SHChieu", 20);
        optionalLength(errors, nmua, path, "DCTDTu", 50);
        optionalLength(errors, nmua, path, "HVTNMHang", 100);
        optionalLength(errors, nmua, path, "STKNHang", 30);
        optionalLength(errors, nmua, path, "TNHang", 400);
    }

    private static void validateLineItems(List<String> errors, Element dshhdvu) {
        List<Element> rows = children(dshhdvu, "HHDVu");
        if (rows.isEmpty()) {
            errors.add("HDon/DLHDon/NDHDon/DSHHDVu/HHDVu: bắt buộc có ít nhất 1 dòng");
            return;
        }
        int index = 0;
        for (Element row : rows) {
            index++;
            String path = "HDon/DLHDon/NDHDon/DSHHDVu/HHDVu[" + index + "]";
            String tchat = requireNumber(errors, row, path, "TChat", 1, 0);
            optionalNumber(errors, row, path, "STT", 4, 0);
            optionalLength(errors, row, path, "MHHDVu", 50);
            requireText(errors, row, path, "THHDVu", 500);
            optionalLength(errors, row, path, "DVTinh", 50);
            optionalNumber(errors, row, path, "SLuong", 21, 6);
            optionalNumber(errors, row, path, "DGia", 21, 6);
            optionalNumber(errors, row, path, "TLCKhau", 6, 4);
            optionalNumber(errors, row, path, "STCKhau", 21, 6);
            if ("4".equals(tchat)) {
                optionalNumber(errors, row, path, "ThTien", 21, 6);
            } else {
                requireNumber(errors, row, path, "ThTien", 21, 6);
            }
            optionalLength(errors, row, path, "TSuat", 11);
            validateSpecialGoods(errors, child(row, "TTHHDTrung"), path + "/TTHHDTrung", "5".equals(tchat));
        }
    }

    private static void validateSpecialGoods(List<String> errors, Element special, String path, boolean required) {
        if (special == null) {
            if (required) errors.add(path + ": bắt buộc khi TChat=5");
            return;
        }
        if (!hasAnyElementText(special) && !required) return;
        requireNumber(errors, special, path, "LHHDTrung", 1, 0);
        List<Element> details = children(special, "TTin");
        if (details.isEmpty()) {
            errors.add(path + "/TTin: bắt buộc khi có thông tin hàng hóa đặc trưng");
            return;
        }
        int index = 0;
        for (Element detail : details) {
            index++;
            String detailPath = path + "/TTin[" + index + "]";
            requireText(errors, detail, detailPath, "TTruong", 20);
            requireText(errors, detail, detailPath, "DLieu", 200);
        }
    }

    private static void validatePayment(List<String> errors, Element ttoan) {
        String path = "HDon/DLHDon/NDHDon/TToan";
        Element thttltsuat = child(ttoan, "THTTLTSuat");
        if (thttltsuat != null) {
            int index = 0;
            for (Element ltsuat : children(thttltsuat, "LTSuat")) {
                index++;
                String ratePath = path + "/THTTLTSuat/LTSuat[" + index + "]";
                optionalLength(errors, ltsuat, ratePath, "TSuat", 11);
                requireNumber(errors, ltsuat, ratePath, "ThTien", 21, 6);
                optionalNumber(errors, ltsuat, ratePath, "TThue", 21, 6);
            }
        }
        requireNumber(errors, ttoan, path, "TgTCThue", 21, 6);
        optionalNumber(errors, ttoan, path, "TGTKCThue", 21, 6);
        requireNumber(errors, ttoan, path, "TgTThue", 21, 6);
        validateFees(errors, child(ttoan, "DSLPhi"), path + "/DSLPhi");
        optionalNumber(errors, ttoan, path, "TTCKTMai", 21, 6);
        optionalNumber(errors, ttoan, path, "TGTKhac", 21, 6);
        requireNumber(errors, ttoan, path, "TgTTTBSo", 21, 6);
        requireText(errors, ttoan, path, "TgTTTBChu", 255);
    }

    private static void validateFees(List<String> errors, Element fees, String path) {
        if (fees == null) return;
        int index = 0;
        for (Element fee : children(fees, "LPhi")) {
            index++;
            String feePath = path + "/LPhi[" + index + "]";
            requireText(errors, fee, feePath, "TLPhi", 100);
            requireNumber(errors, fee, feePath, "TPhi", 21, 6);
        }
    }

    private static void validateDscks(List<String> errors, Element dscks) {
        if (dscks == null) return;
        Element nban = child(dscks, "NBan");
        if (nban == null || !hasDescendant(nban, "Signature")) {
            errors.add("HDon/DSCKS/NBan/Signature: bắt buộc có chữ ký số người bán trước khi gửi CQT");
        }
        Element cqt = child(dscks, "CQT");
        if (cqt != null && hasAnyElementText(cqt) && !hasDescendant(cqt, "Signature")) {
            errors.add("HDon/DSCKS/CQT/Signature: dữ liệu chữ ký CQT không đúng cấu trúc");
        }
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

    private static String optionalDate(List<String> errors, Element parent, String path, String tag) {
        Element child = child(parent, tag);
        String value = text(child);
        if (hasText(value)) {
            validateDate(errors, path + "/" + tag, value);
        }
        return value;
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
        String normalized = value.trim();
        try {
            LocalDate.parse(normalized);
            return;
        } catch (Exception ignore) {}
        try {
            LocalDateTime.parse(normalized);
            return;
        } catch (Exception ignore) {}
        errors.add(path + ": phải là ngày hợp lệ theo định dạng yyyy-MM-dd");
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

    private static boolean hasDescendant(Element parent, String tag) {
        if (parent == null) return false;
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes != null && nodes.getLength() > 0) return true;
        NodeList all = parent.getElementsByTagName("*");
        for (int i = 0; i < all.getLength(); i++) {
            if (tag.equals(tagName(all.item(i)))) return true;
        }
        return false;
    }

    private static boolean hasAnyElementText(Element parent) {
        if (parent == null) return false;
        if (hasText(text(parent))) {
            NodeList childNodes = parent.getChildNodes();
            for (int i = 0; i < childNodes.getLength(); i++) {
                if (childNodes.item(i) instanceof Element && hasAnyElementText((Element) childNodes.item(i))) {
                    return true;
                }
            }
            return parent.getChildNodes().getLength() == 1;
        }
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i) instanceof Element && hasAnyElementText((Element) nodes.item(i))) {
                return true;
            }
        }
        return false;
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
            return new Result(true, "XML hóa đơn hợp lệ");
        }

        private static Result invalid(List<String> errors) {
            List<String> safeErrors = errors == null ? Collections.emptyList() : errors;
            if (safeErrors.isEmpty()) {
                return new Result(false, "XML hóa đơn không hợp lệ");
            }
            int limit = Math.min(5, safeErrors.size());
            String message = "XML hóa đơn không hợp lệ: " + String.join("; ", safeErrors.subList(0, limit));
            if (safeErrors.size() > limit) {
                message += "; ...";
            }
            return new Result(false, message);
        }
    }
}
