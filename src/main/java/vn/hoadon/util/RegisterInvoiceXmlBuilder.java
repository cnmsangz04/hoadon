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

        // Parse JSON fields from entity
        Set<String> invoiceForms = parseStringSet(e.getInvoiceForms());
        Set<String> sendMethodCodes = parseSendMethods(e.getSendMethods());
        Set<String> transferMethods = parseStringSet(e.getTransferMethods());
        Set<String> invoiceTypes = parseStringSet(e.getInvoiceTypes());
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

    private static Set<String> parseStringSet(String json) {
        Set<String> out = new HashSet<>();
        if (json == null || json.isBlank()) return out;
        try {
            JsonNode node = parseLenient(json);
            if (node == null) return out;
            fillStringSetFromNode(out, node);
        } catch (Exception ignored) {}
        return out;
    }

    private static void fillStringSetFromNode(Set<String> out, JsonNode node) {
        if (node == null) return;
        if (node.isArray()) {
            for (JsonNode n : node) {
                if (n.isTextual()) out.add(n.asText().trim());
                else if (n.isObject()) {
                    JsonNode code = n.get("code");
                    if (code != null && code.isTextual()) out.add(code.asText().trim());
                    else out.add(n.toString());
                } else if (n.isValueNode()) out.add(n.asText().trim());
            }
        } else if (node.isObject()) {
            // some payloads might be { list: [...] } or similar
            boolean consumed = false;
            for (Iterator<String> it = node.fieldNames(); it.hasNext(); ) {
                String key = it.next();
                JsonNode arr = node.get(key);
                if (arr != null && arr.isArray()) {
                    consumed = true;
                    for (JsonNode n : arr) {
                        if (n.isTextual() || n.isValueNode()) out.add(n.asText().trim());
                    }
                }
            }
            if (!consumed) {
                // fallback: try common fields like code/value
                JsonNode code = node.get("code");
                if (code != null && code.isValueNode()) out.add(code.asText().trim());
            }
        } else if (node.isTextual()) {
            // Try nested JSON string: e.g. "[\"CMa\",...]"
            String txt = node.asText();
            try {
                JsonNode nested = MAPPER.readTree(txt);
                if (nested != null && !nested.isMissingNode()) {
                    fillStringSetFromNode(out, nested);
                } else {
                    out.add(txt.trim());
                }
            } catch (Exception ex) {
                out.add(txt.trim());
            }
        } else if (node.isValueNode()) {
            out.add(node.asText().trim());
        }
    }

    private static Set<String> parseSendMethods(String json) {
        // send_methods could be array, object with keys a,b,c, or a JSON string of those
        Set<String> out = new HashSet<>();
        if (json == null || json.isBlank()) return out;
        try {
            JsonNode node = parseLenient(json);
            if (node == null) return out;
            if (node.isArray()) {
                for (JsonNode n : node) if (n.isValueNode()) out.add(n.asText().trim());
            } else if (node.isObject()) {
                // known keys a,b,c
                for (String key : Arrays.asList("a","b","c","items","list","codes")) {
                    JsonNode arr = node.get(key);
                    if (arr != null && arr.isArray()) {
                        for (JsonNode n : arr) if (n.isValueNode()) out.add(n.asText().trim());
                    }
                }
            } else if (node.isTextual()) {
                // nested JSON string
                try {
                    JsonNode nested = MAPPER.readTree(node.asText());
                    if (nested != null) {
                        if (nested.isArray()) {
                            for (JsonNode n : nested) if (n.isValueNode()) out.add(n.asText().trim());
                        } else if (nested.isObject()) {
                            for (Iterator<String> it = nested.fieldNames(); it.hasNext();) {
                                String key = it.next();
                                JsonNode arr = nested.get(key);
                                if (arr != null && arr.isArray()) for (JsonNode n : arr) if (n.isValueNode()) out.add(n.asText().trim());
                            }
                        } else if (nested.isValueNode()) {
                            out.add(nested.asText().trim());
                        }
                    }
                } catch (Exception ex) {
                    out.add(node.asText().trim());
                }
            }
        } catch (Exception ignored) {}
        return out;
    }

    private static Set<String> mapInvoiceTypeCodes(Set<String> input) {
        if (input == null || input.isEmpty()) return input;
        Set<String> out = new HashSet<>();
        Map<String,String> map = new HashMap<>();
        map.put("HDThuongMai", "HDTMai");
        map.put("HDGTGT_BienLai", "HDGTGTTHBLai");
        map.put("HDBHang_BienLai", "HDBHTHBLai");
        // identity for others
        for (String s : input) {
            out.add(map.getOrDefault(s, s));
        }
        return out;
    }

    private static JsonNode parseLenient(String json) throws JsonProcessingException {
        if (json == null) return null;
        try {
            return MAPPER.readTree(json);
        } catch (JsonProcessingException ex) {
            // if it's quoted JSON string, try once more
            if (json.startsWith("\"") && json.endsWith("\"")) {
                String unq = json.substring(1, json.length()-1)
                        .replace("\\\"", "\"")
                        .replace("\\n", "\n");
                return MAPPER.readTree(unq);
            }
            throw ex;
        }
    }

    private static class CertificateRow {
        String orgName;
        String serialNo;
        String signFromDate;
        String signToDate;
        String sigRegMethod;
    }

    private static List<CertificateRow> parseCertificates(String json) {
        List<CertificateRow> out = new ArrayList<>();
        if (json == null || json.isBlank()) return out;
        try {
            JsonNode node = parseLenient(json);
            if (node == null) return out;
            if (node.isArray()) {
                for (JsonNode n : node) addCertNode(out, n);
            } else if (node.isObject()) {
                // single object
                addCertNode(out, node);
            } else if (node.isTextual()) {
                // try parse nested json string
                try {
                    JsonNode n2 = MAPPER.readTree(node.asText());
                    if (n2 != null) {
                        if (n2.isArray()) for (JsonNode n : n2) addCertNode(out, n);
                        else if (n2.isObject()) addCertNode(out, n2);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return out;
    }

    private static void addCertNode(List<CertificateRow> out, JsonNode n) {
        if (n == null || !n.isObject()) return;
        CertificateRow c = new CertificateRow();
        c.orgName = text(n, "orgName", "org_name", "organizationName");
        c.serialNo = text(n, "serialNo", "serial_no", "serial");
        c.signFromDate = text(n, "signFromDate", "sign_from_date", "from");
        c.signToDate = text(n, "signToDate", "sign_to_date", "to");
        c.sigRegMethod = text(n, "sigRegMethod", "sig_reg_method", "method");
        out.add(c);
    }

    private static String text(JsonNode obj, String... keys) {
        for (String k : keys) {
            JsonNode v = obj.get(k);
            if (v != null && v.isValueNode()) return v.asText();
        }
        return null;
    }
}
