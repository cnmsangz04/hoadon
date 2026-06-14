package vn.hoadon.util;

import vn.hoadon.entity.RegisterInvoiceEntity;

import java.util.UUID;
import java.util.*;

// JSON parsing
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * Build XML for register invoice (ĐKTĐ-HĐĐT) from RegisterInvoiceEntity.
 * This builds an unsigned XML document compatible with CQT template.
 */
public final class RegisterInvoiceXmlBuilder {
    private RegisterInvoiceXmlBuilder() {}

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Build unsigned XML string from entity.
     * Contact info is no longer stored in register_invoices; callers should
     * prefer the overload with explicit contact parameters. This method will
     * render empty contact fields.
     */
    public static String buildUnsigned(RegisterInvoiceEntity e) {
        // Delegate to overload with null contact info
        return buildUnsigned(e, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /** Build unsigned XML using overrides for contact info */
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
     * Build unsigned XML with explicit contact info and tax authority code/name provided by caller.
     * Also require company name and tax code to be passed in by caller.
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
        if (e == null) return "";
        String id = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String pban = "2.1.0";
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
        String ddanh = nullToEmpty(e.getCreatePlace());
        String nlap = e.getDeclarationDate() != null ? e.getDeclarationDate().toString() : java.time.LocalDate.now().toString();

        // Convert list fields from entity
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
          .append(tag("HDGTGT_BienLai", flag(invoiceTypes, "HDGTGT_BienLai")))
          .append(tag("HDBHang", flag(invoiceTypes, "HDBHang")))
          .append(tag("HDBHang_BienLai", flag(invoiceTypes, "HDBHang_BienLai")))
          .append(tag("HDThuongMai", flag(invoiceTypes, "HDThuongMai")))
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
                  .append(tag("TNgay", nullToEmpty(c.signFromDate)))
                  .append(tag("DNgay", nullToEmpty(c.signToDate)))
                  .append(tag("HThuc", nullToEmpty(c.sigRegMethod)))
                  .append("</CTS>");
            }
        }
        sb.append("</DSCTSSDung>");

        // Keep placeholders for providers (not requested to change)
        sb.append("<TTTCGP>");
        sb.append("<TCGP>")
          .append(tag("STT", "1"))
          .append(tag("TTCGP", ""))
          .append(tag("MSTTCGP", ""))
          .append(tag("TNgay", e.getEffectiveDate() != null ? e.getEffectiveDate().toString() : ""))
          .append(tag("DNgay", ""))
          .append(tag("GChu", ""))
          .append("</TCGP>");
        sb.append("</TTTCGP>");
        sb.append("<TTTCTN>");
        sb.append("<TCTN>")
          .append(tag("STT", "1"))
          .append(tag("TTCTN", ""))
          .append(tag("MSTTCTN", ""))
          .append(tag("TNgay", e.getEffectiveDate() != null ? e.getEffectiveDate().toString() : ""))
          .append(tag("DNgay", ""))
          .append(tag("GChu", ""))
          .append("</TCTN>");
        sb.append("</TTTCTN>");
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
                    // Handle malformed data like "{orgName=VNPT, serialNo=123, ...}"
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
