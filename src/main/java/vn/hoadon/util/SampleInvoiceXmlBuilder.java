package vn.hoadon.util;

import vn.hoadon.entity.CompanyBankEntity;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.LegalRepresentativeEntity;
import vn.hoadon.entity.UserEntity;

import java.time.LocalDate;

/**
 * Tạo XML hóa đơn mẫu để xem trước mẫu XSLT.
 * Dữ liệu này cố định, không phụ thuộc DB và chỉ dùng cho chức năng xem trước.
 */
public final class SampleInvoiceXmlBuilder {
    private SampleInvoiceXmlBuilder() {}

    // Biến phiên bản toàn cục
    private static final String PBAN = "2.1.0";

    /**
     * Hàm tạo tương thích ngược, dùng placeholder khi không truyền dữ liệu.
     */
    public static String build() {
        return build(null, null, null, null, null);
    }

    /**
     * Tạo chuỗi XML mẫu từ các entity được truyền vào. Quy tắc ánh xạ:
     * - THDon <- form.name
     * - KHMSHDon <- form.formCode
     * - KHHDon <- form.serial
     * - NLap <- ngày hiện tại (yyyy-MM-dd)
     * - Thông tin công ty lấy từ Company, Bank và Representative
     */
    public static String build(UserEntity user,
                               FormInvoiceEntity form,
                               CompanyEntity company,
                               CompanyBankEntity bank,
                               LegalRepresentativeEntity representative) {
        String thDon = form != null && form.getName() != null ? form.getName() : "";
        String khmshDon = form != null && form.getFormCode() != null ? form.getFormCode() : "";
        String khhDon = form != null && form.getSerial() != null ? form.getSerial() : "";
        String nLap = LocalDate.now().toString();

        String tenCty = company != null && company.getName() != null ? company.getName() : "";
        String mst = company != null && company.getTaxcode() != null ? company.getTaxcode() : "";
        String dchi = company != null && company.getAddress() != null ? company.getAddress() : "";
        String sdt = company != null && company.getInvoicePhone() != null ? company.getInvoicePhone() : "";
        String email = company != null && company.getInvoiceEmail() != null ? company.getInvoiceEmail() : "";
        String fax = company != null && company.getInvoiceFax() != null ? company.getInvoiceFax() : "";
        String website = company != null && company.getInvoiceWebsite() != null ? company.getInvoiceWebsite() : "";
        String accountNo = bank != null && bank.getAccountNumber() != null ? bank.getAccountNumber() : "";
        String bankName = bank != null && bank.getBankName() != null ? bank.getBankName() : "";
        String repName = representative != null && representative.getFullname() != null ? representative.getFullname() : "";

        StringBuilder sb = new StringBuilder(2048);
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<HDon>");
        sb.append("<DLHDon Id=\"\">");
        sb.append("<TTChung>")
          .append(tag("PBan", PBAN))
          .append(tag("THDon", thDon))
          .append(tag("KHMSHDon", khmshDon))
          .append(tag("KHHDon", khhDon))
          .append(tag("SHDon", "0"))
          .append(tag("MHSo", ""))
          .append(tag("NLap", nLap))
          .append(tag("SBKe", ""))
          .append(tag("NBKe", ""))
          .append(tag("DVTTe", ""))
          .append(tag("TGia", ""))
          .append(tag("HTTToan", ""))
          .append(tag("MSTDVNUNLHDon", ""))
          .append(tag("TDVNUNLHDon", ""))
          .append(tag("DCDVNUNLHDon", ""))
          .append("</TTChung>");
        sb.append("<NDHDon>");
        sb.append("<NBan>")
          .append(tag("Ten", tenCty))
          .append(tag("MST", mst))
          .append(tag("DChi", dchi))
          .append(tag("MCHang", ""))
          .append(tag("TCHang", ""))
          .append(tag("SDThoai", sdt))
          .append(tag("DCTDTu", email))
          .append(tag("STKNHang", accountNo))
          .append(tag("TNHang", bankName))
          .append(tag("Fax", fax))
          .append(tag("Website", website))
          .append("<TTKhac>")
            .append("<TTin>")
              .append(tag("TTruong", "NDDPLuat"))
              .append(tag("KDLieu", "string"))
              .append(tag("DLieu", repName))
            .append("</TTin>")
          .append("</TTKhac>")
          .append("</NBan>");
        sb.append("<NMua>")
          .append("</NMua>");
        sb.append("</NDHDon>");
        sb.append("<DSHHDVu>")
          .append("<HHDVu>")
            .append(tag("TChat", ""))
            .append(tag("MHHDVu", ""))
            .append(tag("THHDVu", ""))
            .append(tag("DVTinh", ""))
            .append(tag("SLuong", ""))
            .append(tag("DGia", ""))
            .append(tag("TLCKhau", ""))
            .append(tag("STCKhau", ""))
            .append(tag("ThTien", ""))
            .append(tag("TSuat", ""))
          .append("</HHDVu>")
        .append("</DSHHDVu>");
        sb.append("<TToan>")
          .append("<THTTLTSuat>")
            .append(tag("LTSuat", ""))
          .append("</THTTLTSuat>")
          .append(tag("TgTCThue", ""))
          .append(tag("TgTThue", ""))
          .append("<DSLPhi>")
            .append("<LPhi>")
              .append(tag("TLPhi", ""))
              .append(tag("TPhi", ""))
            .append("</LPhi>")
          .append("</DSLPhi>")
          .append(tag("TTCKTMai", ""))
          .append(tag("TgTTTBSo", ""))
          .append(tag("TgTTTBChu", ""))
        .append("</TToan>");
        sb.append("</DLHDon>");
        sb.append("<DSCKS>")
          .append("<NBan></NBan>")
          .append("<NMua></NMua>")
        .append("</DSCKS>");
        sb.append("</HDon>");
        return sb.toString();
    }

    private static String tag(String name, String value) {
        return "<" + name + ">" + escapeXml(value) + "</" + name + ">";
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
}
