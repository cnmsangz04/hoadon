package vn.hoadon.util;

import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.entity.InvoiceEntity;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.CompanyBankEntity;

import java.util.*;

/**
 * Build XML for an invoice (HDon) based on InvoiceEntity and related company info.
 */
public final class InvoiceXmlBuilder {
    private InvoiceXmlBuilder() {}

    /**
     * Build invoice XML from entities.
     * @param inv InvoiceEntity
     * @param form FormInvoiceEntity (to derive KHHDon)
     * @param company CompanyEntity
     * @param bank Preferred CompanyBankEntity (may be null)
     */
    public static String build(InvoiceEntity inv, FormInvoiceEntity form, CompanyEntity company, CompanyBankEntity bank) {
        if (inv == null) return "";
        String id = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String pban = "2.1.0";
        String thdon = nullToEmpty(inv.getName());
        String khmshdon = form != null && form.getFormCode() != null ? form.getFormCode() : "";
        String khhdon = form != null && form.getSerial() != null ? form.getSerial() : "";
        String shdon = inv.getNo() != null ? String.valueOf(inv.getNo()) : "0";
        String mhso = "";
        String nlap = inv.getDateExport() != null ? inv.getDateExport().toString() : "";
        String sbke = "";
        String nbke = "";
        String dvtte = nullToEmpty(inv.getCurrency());
        String tgia = inv.getExchangeRate() != null ? trimZeros(inv.getExchangeRate()) : "";
        String htttoan = paymentTypeLabel(inv.getPaymentType());
        String msttcgp = company != null ? nullToEmpty(company.getTaxcode()) : "";
        String sellerName = company != null ? nullToEmpty(company.getName()) : "";
        String sellerMst = company != null ? nullToEmpty(company.getTaxcode()) : "";
        String sellerAddr = company != null ? nullToEmpty(company.getAddress()) : "";
        String sellerPhone = company != null ? nullToEmpty(company.getInvoicePhone()) : "";
        String sellerEmail = company != null ? nullToEmpty(company.getInvoiceEmail()) : "";
        String sellerFax = company != null ? nullToEmpty(company.getInvoiceFax()) : "";
        String sellerWebsite = company != null ? nullToEmpty(company.getInvoiceWebsite()) : "";
        String sellerBankNo = bank != null ? nullToEmpty(bank.getAccountNumber()) : "";
        String sellerBankName = bank != null ? (nullToEmpty(bank.getBankName()) + (hasText(bank.getBankBrand()) ? (" " + bank.getBankBrand()) : "")) : "";

        // Parse customer JSON
        Map<String,Object> customer = parseJsonObject(inv.getCustomer());
        String buyerName = deriveBuyerName(customer);
        String buyerFullName = str(customer.get("buyer"));
        String buyerMst = str(customer.get("taxcode"));
        String buyerAddr = str(customer.get("address"));
        String buyerCode = str(customer.get("code"));
        String buyerPhone = str(customer.get("phone"));
        String buyerEmail = str(customer.get("email"));
        String buyerBankNo = str(customer.get("bank_no"));
        String buyerBankName = str(customer.get("bank_name"));

        // Parse detail JSON array
        java.util.List<Map<String,Object>> details = parseJsonArray(inv.getDetail());

        StringBuilder sb = new StringBuilder();
        sb.append("<HDon>");
        sb.append("<DLHDon Id=\"").append(id).append("\">");
        sb.append("<TTChung>")
          .append(tag("PBan", pban))
          .append(tag("THDon", thdon))
          .append(tag("KHMSHDon", khmshdon))
          .append(tag("KHHDon", khhdon))
          .append(tag("SHDon", shdon))
          .append(tag("MHSo", mhso))
          .append(tag("NLap", nlap))
          .append(tag("SBKe", sbke))
          .append(tag("NBKe", nbke))
          .append(tag("DVTTe", dvtte))
          .append(tag("TGia", tgia))
          .append(tag("HTTToan", htttoan))
          .append(tag("MSTTCGP", msttcgp))
          .append(tag("MSTDVNUNLHDon", ""))
          .append(tag("TDVNUNLHDon", ""))
          .append(tag("DCDVNUNLHDon", ""))
          .append(tag("TTKhac", ""))
          .append("<TTHDLQuan>")
          .append(tag("TCHDon", ""))
          .append(tag("LHDCLQuan", ""))
          .append(tag("KHMSHDCLQuan", ""))
          .append(tag("KHHDCLQuan", ""))
          .append(tag("SHDCLQuan", ""))
          .append(tag("NLHDCLQuan", ""))
          .append(tag("GChu", ""))
          .append(tag("SBKCLQuan", ""))
          .append(tag("NBKCLQuan", ""))
          .append("</TTHDLQuan>")
          .append("</TTChung>");
        sb.append("<NDHDon>");
        sb.append("<NBan>")
          .append(tag("Ten", sellerName))
          .append(tag("MST", sellerMst))
          .append(tag("DChi", sellerAddr))
          .append(tag("MCHang", ""))
          .append(tag("TCHang", ""))
          .append(tag("SDThoai", sellerPhone))
          .append(tag("DCTDTu", sellerEmail))
          .append(tag("STKNHang", sellerBankNo))
          .append(tag("TNHang", sellerBankName))
          .append(tag("Fax", sellerFax))
          .append(tag("Website", sellerWebsite))
          .append(tag("TTKhac", ""))
          .append("</NBan>");
        sb.append("<NMua>")
          .append(tag("Ten", buyerName))
          .append(tag("MST", buyerMst))
          .append(tag("MDVQHNSach", ""))
          .append(tag("DChi", buyerAddr))
          .append(tag("MKHang", buyerCode))
          .append(tag("SDThoai", buyerPhone))
          .append(tag("CCCDan", ""))
          .append(tag("SHChieu", ""))
          .append(tag("DCTDTu", buyerEmail))
          .append(tag("HVTNMHang", buyerFullName))
          .append(tag("STKNHang", buyerBankNo))
          .append(tag("TNHang", buyerBankName))
          .append(tag("TTKhac", ""))
          .append("</NMua>");
        sb.append("<DSHHDVu>");
        int idx = 0;
        for (Map<String,Object> d : details) {
          idx++;
          String tchat = toStringOrDefault(d.get("feature"), "");
          String stt = toStringOrDefault(d.get("num"), String.valueOf(idx));
          String code = toStringOrDefault(d.get("code"), "");
          String name = toStringOrDefault(d.get("name"), "");
          String unit = toStringOrDefault(d.get("unit"), "");
          String qty = toStringOrDefault(d.get("quantity"), "");
          String price = toStringOrDefault(d.get("price"), "");
          String tlckhau = toStringOrDefault(d.get("num"), "");
          String stckhau = toStringOrDefault(d.get("num"), "");
          String total = toStringOrDefault(d.get("total"), "");
          String vatRate = formatVatRate(d.get("vatRate"));
          String vatAmount = toStringOrDefault(d.get("vatAmount"), "");
          String amount = toStringOrDefault(d.get("amount"), "");
          sb.append("<HHDVu>")
            .append(tag("TChat", tchat))
            .append(tag("STT", stt))
            .append(tag("MHHDVu", code))
            .append(tag("THHDVu", name))
            .append(tag("DVTinh", unit))
            .append(tag("SLuong", qty))
            .append(tag("DGia", price))
            .append(tag("TLCKhau", tlckhau))
            .append(tag("STCKhau", stckhau))
            .append(tag("ThTien", total))
            .append(tag("TSuat", vatRate))
            .append("<TTKhac>")
            .append("<TTin>")
            .append(tag("TTruong", "TThue"))
            .append(tag("KDLieu", "numeric"))
            .append(tag("DLieu", vatAmount))
            .append("</TTin>")
            .append("<TTin>")
            .append(tag("TTruong", "TCong"))
            .append(tag("KDLieu", "numeric"))
            .append(tag("DLieu", amount))
            .append("</TTin>")
            .append("</TTKhac>")
            .append("</HHDVu>");
        }
        sb.append("</DSHHDVu>");

        // Group totals by VAT rate
        Map<String, Totals> totalsByRate = new LinkedHashMap<>();
        for (Map<String,Object> d : details) {
            String rateKey = formatVatRate(d.get("vatRate"));
            Totals t = totalsByRate.computeIfAbsent(rateKey, k -> new Totals());
            t.thTien += toDouble(d.get("total"));
            t.tThue += toDouble(d.get("vatAmount"));
        }
        sb.append("<TToan>")
          .append("<THTTLTSuat>");
        for (Map.Entry<String, Totals> e : totalsByRate.entrySet()) {
            sb.append("<LTSuat>")
              .append(tag("TSuat", e.getKey()))
              .append(tag("ThTien", trimZeros(e.getValue().thTien)))
              .append(tag("TThue", trimZeros(e.getValue().tThue)))
              .append("</LTSuat>");
        }
        sb.append("</THTTLTSuat>")
          .append(tag("TgTCThue", trimZeros(inv.getTotal())))
          .append(tag("TGTKCThue", "0"))
          .append(tag("TgTThue", trimZeros(inv.getVatAmount())))
          .append("<DSLPhi>")
          .append("</DSLPhi>")
          .append(tag("TTCKTMai", trimZeros(inv.getDiscountAmount())))
          .append(tag("TGTKhac", "0"))
          .append(tag("TgTTTBSo", trimZeros(inv.getAmount())))
          .append(tag("TgTTTBChu", nullToEmpty(inv.getAmountInWords())))
          .append(tag("TTKhac", ""))
          .append("</TToan>");
        sb.append("</NDHDon>");
        // TTKhac with lookup code
        sb.append("<TTKhac>")
          .append("<TTin>")
          .append(tag("TTruong", "MTCuu"))
          .append(tag("KDLieu", "string"))
          .append(tag("DLieu", nullToEmpty(inv.getLookupCode())))
          .append("</TTin>")
          .append("</TTKhac>");
        sb.append("</DLHDon>");
        sb.append("<DLQRCode/>");
        sb.append("<DSCKS><NBan/><NMua/><CCKSKhac/></DSCKS>");
        sb.append("</HDon>");
        return sb.toString();
    }

    private static String paymentTypeLabel(Short paymentType) {
        if (paymentType == null) return "";
        switch (paymentType) {
            case 1: return "Tiền mặt";
            case 2: return "Chuyển khoản";
            case 3: return "Tiền mặt/Chuyển khoản";
            default: return "";
        }
    }
    private static String trimZeros(Double v) { if (v == null) return ""; String s = String.valueOf(v); if (s.endsWith(".0")) s = s.substring(0, s.length()-2); return s; }
    private static String trimZeros(Number v) { if (v == null) return ""; String s = String.valueOf(v); if (s.endsWith(".0")) s = s.substring(0, s.length()-2); return s; }
    private static String nullToEmpty(String s) { return s == null ? "" : s; }
    private static boolean hasText(String s) { return s != null && !s.trim().isEmpty(); }

    private static String tag(String name, String value) {
        return "<" + name + ">" + escapeXml(value) + "</" + name + ">";
    }
    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static Map<String,Object> parseJsonObject(String json) {
        if (!hasText(json)) return java.util.Collections.emptyMap();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, java.util.Map.class);
        } catch (Exception ex) {
            return java.util.Collections.emptyMap();
        }
    }
    private static java.util.List<Map<String,Object>> parseJsonArray(String json) {
        if (!hasText(json)) return java.util.Collections.emptyList();
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.List<java.util.Map<String,Object>>>(){});
        } catch (Exception ex) {
            return java.util.Collections.emptyList();
        }
    }
    private static String deriveBuyerName(Map<String,Object> customer) {
        String name = str(customer.get("name"));
        String buyer = str(customer.get("buyer"));
        if (hasText(name)) return name;
        if (hasText(buyer)) return buyer;
        return "";
    }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static String toStringOrDefault(Object o, String def) { String s = str(o); return hasText(s) ? s : def; }
    private static String formatVatRate(Object rateObj) {
        if (rateObj == null) return "";
        try {
            int r = Integer.parseInt(String.valueOf(rateObj));
            return switch (r) {
                case -1 -> "KCT";
                case 0 -> "0%";
                case 5 -> "5%";
                case 8 -> "8%";
                case 10 -> "10%";
                default -> String.valueOf(r);
            };
        } catch (Exception ex) {
            return String.valueOf(rateObj);
        }
    }
    private static double toDouble(Object o) {
        if (o == null) return 0d;
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception ex) { return 0d; }
    }
    private static class Totals { double thTien = 0d; double tThue = 0d; }
}