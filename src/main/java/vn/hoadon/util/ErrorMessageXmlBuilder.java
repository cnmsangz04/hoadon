package vn.hoadon.util;

import vn.hoadon.entity.ErrorMessageEntity;
import vn.hoadon.entity.ErrorMessageInvoiceEntity;

import java.time.format.DateTimeFormatter;
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

    public static String buildHtml(ErrorMessageEntity e) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!doctype html><html><head><meta charset=\"UTF-8\"/>")
          .append("<style>body{font-family:DejaVu Sans,Arial,sans-serif;font-size:12px;color:#111;padding:24px}")
          .append("h1{text-align:center;font-size:18px;margin:10px 0 16px} .center{text-align:center}.bold{font-weight:700}")
          .append("table{width:100%;border-collapse:collapse;margin-top:12px}th,td{border:1px solid #333;padding:6px;vertical-align:top}th{background:#f1f5f9}")
          .append(".sign{margin-top:28px;text-align:right;padding-right:60px}</style></head><body>");
        sb.append("<div class=\"center bold\">CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM</div>");
        sb.append("<div class=\"center\">Độc lập - Tự do - Hạnh phúc</div>");
        sb.append("<h1>THÔNG BÁO HÓA ĐƠN ĐIỆN TỬ CÓ SAI SÓT</h1>");
        sb.append("<p><b>Mẫu số:</b> ").append(xml(e.getFormPattern())).append("</p>");
        sb.append("<p><b>Loại thông báo:</b> ").append(e.getNotificationType() != null && e.getNotificationType() == 2 ? "Giải trình theo thông báo của CQT" : "Thông báo của NNT").append("</p>");
        sb.append("<p><b>Cơ quan thuế:</b> ").append(xml(e.getTaxAuthorityName())).append(" (").append(xml(e.getTaxAuthorityCode())).append(")</p>");
        sb.append("<p><b>Người nộp thuế:</b> ").append(xml(e.getTaxpayerName())).append(" - <b>MST:</b> ").append(xml(e.getTaxCode())).append("</p>");
        sb.append("<p><b>Địa danh:</b> ").append(xml(e.getCreatePlace())).append(" - <b>Ngày lập:</b> ").append(e.getNoticeDate() != null ? e.getNoticeDate() : "").append("</p>");
        sb.append("<table><thead><tr><th>STT</th><th>Mã CQT</th><th>Mẫu số</th><th>Ký hiệu</th><th>Số HĐ</th><th>Ngày HĐ</th><th>Loại</th><th>Lý do</th></tr></thead><tbody>");
        int i = 1;
        if (e.getInvoices() != null) {
            for (ErrorMessageInvoiceEntity line : e.getInvoices()) {
                sb.append("<tr><td class=\"center\">").append(i++).append("</td>")
                  .append("<td>").append(xml(line.getTaxCode())).append("</td>")
                  .append("<td>").append(xml(line.getFormSymbol())).append("</td>")
                  .append("<td>").append(xml(line.getSerial())).append("</td>")
                  .append("<td>").append(xml(line.getInvoiceNo())).append("</td>")
                  .append("<td>").append(line.getInvoiceDate() != null ? line.getInvoiceDate() : "").append("</td>")
                  .append("<td>").append(line.getInvoiceType() != null ? line.getInvoiceType() : "").append("</td>")
                  .append("<td>").append(xml(line.getReason())).append("</td></tr>");
            }
        }
        sb.append("</tbody></table>");
        sb.append("<div class=\"sign\"><b>Người nộp thuế</b><br/><i>Chữ ký số, chữ ký điện tử</i>");
        if (e.getSignDate() != null) {
            sb.append("<br/>Đã ký: ").append(e.getSignDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        }
        sb.append("</div></body></html>");
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
