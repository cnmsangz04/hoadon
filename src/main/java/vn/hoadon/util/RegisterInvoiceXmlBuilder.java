package vn.hoadon.util;

import vn.hoadon.entity.RegisterInvoiceEntity;

import java.util.UUID;
import java.util.*;

// Xử lý JSON
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Tạo XML đăng ký hóa đơn (ĐKTĐ-HĐĐT) từ RegisterInvoiceEntity.
 * Hàm này tạo tài liệu XML chưa ký, tương thích với mẫu CQT.
 */
public final class RegisterInvoiceXmlBuilder {
    private RegisterInvoiceXmlBuilder() {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Tạo chuỗi XML chưa ký từ entity.
     * Thông tin liên hệ không còn lưu trong register_invoices, nên nơi gọi nên
     * dùng overload truyền rõ các tham số liên hệ. Hàm này sẽ render trống các trường liên hệ.
     */
    public static String buildUnsigned(RegisterInvoiceEntity e) {
        // Chuyển sang overload với thông tin liên hệ null.
        return buildUnsigned(e, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /** Tạo XML chưa ký bằng thông tin liên hệ được truyền đè. */
    public static String buildUnsigned(RegisterInvoiceEntity e,
                                       String contactName,
                                       String contactPhone,
                                       String contactEmail,
                                       String contactAddress,
                                       String citizenId,
                                       String passportNo,
                                       String dateOfBirth,
                                       String gender) {
        return buildUnsigned(e, contactName, contactPhone, contactEmail, contactAddress, citizenId, passportNo, dateOfBirth, gender, null, null, null, null);
    }

    /**
     * Tạo XML chưa ký với thông tin liên hệ và mã/tên cơ quan thuế do nơi gọi truyền vào.
     * Tên công ty và mã số thuế cũng được nơi gọi truyền vào.
     */
    public static String buildUnsigned(RegisterInvoiceEntity e,
                                       String contactName,
                                       String contactPhone,
                                       String contactEmail,
                                       String contactAddress,
                                       String citizenId,
                                       String passportNo,
                                       String dateOfBirth,
                                       String gender,
                                       String taxAuthorityCode,
                                       String taxAuthorityName,
                                       String companyName,
                                       String taxCode) {
        return buildUnsigned(e, contactName, contactPhone, contactEmail, contactAddress, citizenId, passportNo,
                dateOfBirth, gender, taxAuthorityCode, taxAuthorityName, companyName, taxCode, null);
    }

    public static String buildUnsigned(RegisterInvoiceEntity e,
                                       String contactName,
                                       String contactPhone,
                                       String contactEmail,
                                       String contactAddress,
                                       String citizenId,
                                       String passportNo,
                                       String dateOfBirth,
                                       String gender,
                                       String taxAuthorityCode,
                                       String taxAuthorityName,
                                       String companyName,
                                       String taxCode,
                                       String createPlaceName) {
        return buildUnsigned(e, contactName, contactPhone, contactEmail, contactAddress, citizenId, passportNo,
                dateOfBirth, gender, taxAuthorityCode, taxAuthorityName, companyName, taxCode, createPlaceName, null);
    }

    public static String buildUnsigned(RegisterInvoiceEntity e,
                                       String contactName,
                                       String contactPhone,
                                       String contactEmail,
                                       String contactAddress,
                                       String citizenId,
                                       String passportNo,
                                       String dateOfBirth,
                                       String gender,
                                       String taxAuthorityCode,
                                       String taxAuthorityName,
                                       String companyName,
                                       String taxCode,
                                       String createPlaceName,
                                       String documentId) {
        if (e == null) return "";
        String id = documentId != null && !documentId.isBlank()
                ? documentId.trim()
                : e.getIdAttr() != null && !e.getIdAttr().isBlank()
                    ? e.getIdAttr().trim()
                    : UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String pban = "2.1.1";
        String mso = e.getFormPattern() != null ? e.getFormPattern() : "01/ĐKTĐ-HĐĐT";
        String ten = "Tờ khai đăng ký/thay đổi thông tin sử dụng hóa đơn điện tử";
        String hthuc = String.valueOf(e.getDeclarationType() == null ? 1 : e.getDeclarationType());
        String tnnt = nullToEmpty(companyName);
        String mst = nullToEmpty(taxCode);
        String cqtqly = nullToEmpty(taxAuthorityName);
        String mcqtqly = nullToEmpty(taxAuthorityCode);
        String tnddpluat = nullToEmpty(contactName);
        String dtddpluat = nullToEmpty(contactPhone);
        String cccdan = nullToEmpty(citizenId);
        String shchieu = nullToEmpty(passportNo);
        String nsddpluat = nullToEmpty(dateOfBirth);
        String gtinh = nullToEmpty(gender);
        String dclhe = nullToEmpty(contactAddress);
        String dctdtu = nullToEmpty(contactEmail);
        String nlhe = nullToEmpty(contactName);
        String dtlhe = nullToEmpty(contactPhone);
        String ddanh = firstNonBlank(createPlaceName, e.getCreatePlace());
        String nlap = e.getDeclarationDate() != null ? e.getDeclarationDate().toString() : java.time.LocalDate.now().toString();

        // Chuyển các trường dạng danh sách từ entity
        Set<String> invoiceForms = toSet(e.getInvoiceForms());
        Set<String> sendMethodCodes = toSet(e.getSendMethods());
        Set<String> transferMethods = toSet(e.getTransferMethods());
        Set<String> invoiceTypes = toSet(e.getInvoiceTypes());
        List<CertificateRow> certs = parseCertificates(e.getDigitalCertificates());

        StringBuilder sb = new StringBuilder();
        sb.append("<TKhai>");
        sb.append("<DLTKhai Id=\"").append(id).append("\">");
        sb.append("<TTChung>")
          .append(tag("PBan", pban))
          .append(tag("MSo", mso))
          .append(tag("Ten", ten))
          .append(tag("HThuc", hthuc))
          .append(tag("TNNT", tnnt))
          .append(tag("MST", mst))
          .append(tag("CQTQLy", cqtqly))
          .append(tag("MCQTQLy", mcqtqly))
          .append(tag("TNDDPLuat", tnddpluat))
          .append(tag("DTDDPLuat", dtddpluat))
          .append(tag("CCCDan", cccdan))
          .append(tag("SHChieu", shchieu))
          .append(tag("NSDDPLuat", nsddpluat))
          .append(tag("GTinh", gtinh))
          .append(tag("DCLHe", dclhe))
          .append(tag("DCTDTu", dctdtu))
          .append(tag("NLHe", nlhe))
          .append(tag("DTLHe", dtlhe))
          .append(tag("DDanh", ddanh))
          .append(tag("NLap", nlap))
          .append("</TTChung>");
        sb.append("<NDTKhai>");
        sb.append("<HTHDon>")
          .append(tag("CMa", flag(invoiceForms, "CMa")))
          .append(tag("KCMa", flag(invoiceForms, "KCMa")))
          .append(tag("CMTMTTien", flag(invoiceForms, "CMTMTTien")))
          .append("</HTHDon>");
        sb.append("<HTGDLHDDT>")
          .append(tag("NNTDBKKhan", flag(sendMethodCodes, "NNTDBKKhan")))
          .append(tag("NNTKTDNUBND", flag(sendMethodCodes, "NNTKTDNUBND")))
          .append(tag("CQXLTSCong", flag(sendMethodCodes, "CQXLTSCong", "CQT")))
          .append(tag("CDLTTDCQT", flag(sendMethodCodes, "CDLTTDCQT")))
          .append(tag("CDLQTCTN", flag(sendMethodCodes, "CDLQTCTN")))
          .append("</HTGDLHDDT>");
        sb.append("<PThuc>")
          .append(tag("CDDu", flag(transferMethods, "CDDu")))
          .append(tag("CBTHop", flag(transferMethods, "CBTHop")))
          .append("</PThuc>");
        sb.append("<LHDSDung>")
          .append(tag("HDGTGT", flag(invoiceTypes, "HDGTGT")))
          .append(tag("HDGTGTTHBLai", flag(invoiceTypes, "HDGTGTTHBLai", "HDGTGT_BienLai")))
          .append(tag("HDBHang", flag(invoiceTypes, "HDBHang")))
          .append(tag("HDBHTHBLai", flag(invoiceTypes, "HDBHTHBLai", "HDBHang_BienLai")))
          .append(tag("HDTMai", flag(invoiceTypes, "HDTMai", "HDThuongMai")))
          .append(tag("HDBTSCong", flag(invoiceTypes, "HDBTSCong")))
          .append(tag("HDBHDTQGia", flag(invoiceTypes, "HDBHDTQGia")))
          .append(tag("HDKhac", flag(invoiceTypes, "HDKhac")))
          .append(tag("CTu", flag(invoiceTypes, "CTu")))
          .append("</LHDSDung>");
        sb.append("<DSCTSSDung>");
        if (certs.isEmpty()) {
            // no entries
        } else {
            int stt = 1;
            for (CertificateRow c : certs) {
                sb.append("<CTS>")
                  .append(tag("STT", String.valueOf(stt++)))
                  .append(tag("TTChuc", nullToEmpty(c.orgName)))
                  .append(tag("Seri", nullToEmpty(c.serialNo)))
                  .append(tag("TNgay", xmlDateTime(c.signFromDate)))
                  .append(tag("DNgay", xmlDateTime(c.signToDate)))
                  .append(tag("HThuc", nullToEmpty(c.sigRegMethod)))
                  .append("</CTS>");
            }
        }
        sb.append("</DSCTSSDung>");

        appendProviderGroups(sb, e);
        sb.append("</NDTKhai>");
        sb.append("</DLTKhai>");
        sb.append("<DSCKS><NNT></NNT></DSCKS>");
        sb.append("</TKhai>");
        return sb.toString();
    }

    private static String tag(String name, String value) {
        return "<" + name + ">" + escapeXml(value) + "</" + name + ">";
    }
    private static String nullToEmpty(String s) { return s == null ? "" : s; }

    private static String xmlDateTime(String value) {
        String normalized = nullToEmpty(value).trim();
        if (normalized.isEmpty()) return "";
        if (normalized.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return normalized + "T00:00:00";
        }
        if (normalized.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            return normalized.replace(' ', 'T');
        }
        return normalized;
    }

    private static void appendProviderGroups(StringBuilder sb, RegisterInvoiceEntity e) {
        List<ProviderRow> solutionProviders = parseProviders(e.getSolutionProviders());
        if (!solutionProviders.isEmpty()) {
            appendProviderGroup(sb, "TTTCGP", "TCGP", solutionProviders, true);
        }

        List<ProviderRow> transmitProviders = parseProviders(e.getTransmitProviders());
        if (!transmitProviders.isEmpty()) {
            appendProviderGroup(sb, "TTTCTN", "TCTN", transmitProviders, false);
        }
    }

    private static void appendProviderGroup(StringBuilder sb, String groupTag, String rowTag,
                                            List<ProviderRow> rows, boolean solutionProvider) {
        sb.append('<').append(groupTag).append('>');
        int index = 1;
        for (ProviderRow row : rows) {
            sb.append('<').append(rowTag).append('>')
              .append(tag("STT", String.valueOf(index++)))
              .append(tag(solutionProvider ? "TTCGP" : "TTCTN", nullToEmpty(row.name)))
              .append(tag(solutionProvider ? "MSTTCGP" : "MSTTCTN", nullToEmpty(row.taxCode)))
              .append(tag("TNgay", nullToEmpty(row.fromDate)))
              .append(tag("DNgay", nullToEmpty(row.toDate)))
              .append(tag("GChu", nullToEmpty(row.note)))
              .append("</").append(rowTag).append('>');
        }
        sb.append("</").append(groupTag).append('>');
    }
    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) return preferred.trim();
        return fallback == null ? "" : fallback.trim();
    }
    private static String escapeXml(String s) {
        if (s == null) return "";
        return s
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String flag(Set<String> set, String... anyOf) {
        for (String k : anyOf) {
            if (set.contains(k)) return "1";
        }
        return "0";
    }

    private static Set<String> toSet(List<String> list) {
        Set<String> out = new HashSet<>();
        if (list == null) return out;
        for (String s : list) {
            if (s == null) continue;
            String v = s.trim();
            if (!v.isEmpty()) out.add(v);
        }
        return out;
    }

    private static class CertificateRow {
        String orgName;
        String serialNo;
        String signFromDate;
        String signToDate;
        String sigRegMethod;
    }

    private static class ProviderRow {
        String name;
        String taxCode;
        String fromDate;
        String toDate;
        String note;
    }

    private static List<ProviderRow> parseProviders(List<String> list) {
        List<ProviderRow> result = new ArrayList<>();
        if (list == null || list.isEmpty()) return result;

        for (String item : list) {
            if (item == null || item.trim().isEmpty()) continue;
            try {
                JsonNode node = MAPPER.readTree(item);
                ProviderRow row = new ProviderRow();
                row.name = getJsonString(node, "organName", "TTCGP", "TTCTN", "name");
                row.taxCode = getJsonString(node, "organTaxCode", "MSTTCGP", "MSTTCTN", "taxCode");
                row.fromDate = getJsonString(node, "organFromDate", "fromDate", "TNgay");
                row.toDate = getJsonString(node, "organToDate", "toDate", "DNgay");
                row.note = getJsonString(node, "note", "GChu");
                if (hasProviderData(row)) result.add(row);
            } catch (JsonProcessingException ignored) {}
        }
        return result;
    }

    private static boolean hasProviderData(ProviderRow row) {
        return row != null && (hasText(row.name) || hasText(row.taxCode) || hasText(row.fromDate)
                || hasText(row.toDate) || hasText(row.note));
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static List<CertificateRow> parseCertificates(List<String> list) {
        List<CertificateRow> result = new ArrayList<>();
        if (list == null || list.isEmpty()) {
            return result;
        }
        
        for (String item : list) {
            if (item == null || item.trim().isEmpty()) {
                continue;
            }
            
            try {
                CertificateRow cert = new CertificateRow();
                
                // Thử parse dạng JSON trước
                try {
                    JsonNode node = MAPPER.readTree(item);
                    cert.orgName = getJsonString(node, "orgName", "org_name", "organizationName");
                    cert.serialNo = getJsonString(node, "serialNo", "serial_no", "serial");
                    cert.signFromDate = getJsonString(node, "signFromDate", "sign_from_date", "from");
                    cert.signToDate = getJsonString(node, "signToDate", "sign_to_date", "to");
                    cert.sigRegMethod = getJsonString(node, "sigRegMethod", "sig_reg_method", "method");
                } catch (JsonProcessingException e) {
                    // Xử lý dữ liệu sai định dạng như "{orgName=VNPT, serialNo=123, ...}"
                    cert = parseKeyValueFormat(item);
                }
                
                // Kiểm tra đã có đủ dữ liệu tối thiểu
                if (cert.orgName != null && !cert.orgName.trim().isEmpty() && 
                    cert.serialNo != null && !cert.serialNo.trim().isEmpty()) {
                    result.add(cert);
                }
                
            } catch (Exception e) {
                System.err.println("Failed to parse certificate: " + item + ", error: " + e.getMessage());
            }
        }
        
        return result;
    }
    
    private static String getJsonString(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            JsonNode field = node.get(fieldName);
            if (field != null && !field.isNull()) {
                return field.asText();
            }
        }
        return null;
    }
    
    private static CertificateRow parseKeyValueFormat(String input) {
        CertificateRow cert = new CertificateRow();
        
        // Bỏ cặp ngoặc nhọn ngoài cùng nếu có
        String cleanInput = input.trim().replaceAll("^\\{|\\}$", "");
        
        // Tách theo dấu phẩy nhưng vẫn xử lý cấu trúc lồng nhau
        String[] pairs = cleanInput.split(",\\s*(?![^{}]*})");
        
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                String key = keyValue[0].trim();
                String value = keyValue[1].trim();
                
                switch (key) {
                    case "orgName":
                    case "org_name":
                    case "organizationName":
                        cert.orgName = value;
                        break;
                    case "serialNo":
                    case "serial_no":
                    case "serial":
                        cert.serialNo = value;
                        break;
                    case "signFromDate":
                    case "sign_from_date":
                    case "from":
                        cert.signFromDate = value;
                        break;
                    case "signToDate":
                    case "sign_to_date":
                    case "to":
                        cert.signToDate = value;
                        break;
                    case "sigRegMethod":
                    case "sig_reg_method":
                    case "method":
                        cert.sigRegMethod = value;
                        break;
                }
            }
        }
        
        return cert;
    }

    // Legacy helpers retained in case other callers use them
    private static JsonNode parseLenient(String json) throws JsonProcessingException {
        if (json == null) return null;
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException ex) {
            if (json.startsWith("\"") && json.endsWith("\"")) {
                String unq = json.substring(1, json.length()-1)
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n");
                return MAPPER.readTree(unq);
            }
            throw ex;
        }
    }
}
