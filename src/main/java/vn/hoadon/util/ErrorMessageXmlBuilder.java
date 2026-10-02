package vn.hoadon.util;

import vn.hoadon.entity.ErrorMessageEntity;
import vn.hoadon.entity.ErrorMessageInvoiceEntity;

import java.util.UUID;

public final class ErrorMessageXmlBuilder {
    private ErrorMessageXmlBuilder() {}

    public static final String VERSION = "2.1.1";

    public static String buildUnsigned(ErrorMessageEntity e) {
        if (e == null) return "";
        String id = nonBlank(e.getIdAttr(), UUID.randomUUID().toString().replace("-", "").toUpperCase());
        StringBuilder sb = new StringBuilder();
        sb.append("<TBao>");
        sb.append("<DLTBao Id=\"").append(xml(id)).append("\">");
        sb.append(tag("PBan", VERSION));
        sb.append(tag("MSo", nonBlank(e.getFormPattern(), "04/SS-HĐĐT")));
        sb.append(tag("Ten", "Thông báo hóa đơn điện tử có sai sót"));
        sb.append(tag("Loai", String.valueOf(e.getNotificationType() == null ? 1 : e.getNotificationType())));
        sb.append(tag("So", e.getNotificationType() != null && e.getNotificationType() == 2 ? e.getTaxNoticeNumber() : ""));
        sb.append(tag("NTBCCQT", e.getNotificationType() != null && e.getNotificationType() == 2 && e.getTaxNoticeDate() != null ? e.getTaxNoticeDate().toString() : ""));
        sb.append(tag("TCQT", e.getTaxAuthorityName()));
        sb.append(tag("MCQT", e.getTaxAuthorityCode()));
        sb.append(tag("MST", e.getTaxCode()));
        sb.append(tag("TNNT", e.getTaxpayerName()));
        sb.append(tag("DDanh", e.getCreatePlace()));
        sb.append(tag("NTBao", e.getNoticeDate() != null ? e.getNoticeDate().toString() : java.time.LocalDate.now().toString()));
        sb.append("<DSHDon>");
        int i = 1;
        if (e.getInvoices() != null) {
            for (ErrorMessageInvoiceEntity line : e.getInvoices()) {
                if (line == null) continue;
                sb.append("<HDon>");
                sb.append(tag("STT", String.valueOf(i++)));
                sb.append(tag("MCCQT", line.getTaxCode()));
                sb.append(tag("KHMSHDon", line.getFormSymbol()));
                sb.append(tag("KHHDon", line.getSerial()));
                sb.append(tag("SHDon", line.getInvoiceNo()));
                sb.append(tag("Ngay", line.getInvoiceDate() != null ? line.getInvoiceDate().toString() : ""));
                sb.append(tag("LADHDDT", String.valueOf(line.getInvoiceType() == null ? 1 : line.getInvoiceType())));
                sb.append(tag("LDo", line.getReason()));
                sb.append("</HDon>");
            }
        }
        sb.append("</DSHDon>");
        sb.append("</DLTBao>");
        sb.append("<DSCKS><NNT></NNT></DSCKS>");
        sb.append("</TBao>");
        return sb.toString();
    }

    private static String tag(String name, String value) {
        return "<" + name + ">" + xml(value) + "</" + name + ">";
    }

    private static String nonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private static String xml(Object raw) {
        String s = raw == null ? "" : String.valueOf(raw);
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
