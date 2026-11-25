package vn.hoadon.util;

import vn.hoadon.invoice.template.service.InvoiceTemplateBindingService;

import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceEntity;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.CompanyBankEntity;

import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Tạo XML hóa đơn (HDon) dựa trên InvoiceEntity và thông tin công ty liên quan.
 */
public final class InvoiceXmlBuilder {
    private InvoiceXmlBuilder() {}

    /**
     * Tạo XML hóa đơn từ các entity.
     * @param inv entity hóa đơn
     * @param form entity mẫu hóa đơn, dùng để suy ra KHHDon
     * @param company entity công ty
     * @param bank entity ngân hàng ưu tiên của công ty, có thể null
     */
    public static String build(InvoiceEntity inv, FormInvoiceEntity form, CompanyEntity company, CompanyBankEntity bank) {
        return build(inv, form, company, bank, null, null);
    }

    public static String build(InvoiceEntity inv, FormInvoiceEntity form, CompanyEntity company, CompanyBankEntity bank,
                               InvoiceEntity relatedInvoice, FormInvoiceEntity relatedForm) {
        if (inv == null) return "";
        form = InvoiceTemplateBindingService.frozenForm(inv, form);
        relatedForm = InvoiceTemplateBindingService.frozenForm(relatedInvoice, relatedForm);
        if (form != null && Integer.valueOf(6).equals(form.getCategory())) {
            return buildStockIssue(inv, form, company, bank, relatedInvoice, relatedForm);
        }
        String id = hasText(inv.getIdAttr())
                ? inv.getIdAttr()
                : java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String pban = "2.1.1";
        boolean salesInvoice = form != null && Integer.valueOf(2).equals(form.getCategory());
        String thdon = invoiceTitle(inv, form);
        String khmshdon = form != null && form.getFormCode() != null ? form.getFormCode() : "";
        String khhdon = form != null && form.getSerial() != null ? form.getSerial() : "";
        String shdon = inv.getNo() != null ? String.valueOf(inv.getNo()) : "0";
        String mhso = "";
        String nlap = inv.getDateExport() != null ? inv.getDateExport().toString() : "";
        String hdcttChinh = "0";
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
        RelatedInvoiceXml relatedXml = buildRelatedInvoiceXml(inv, relatedInvoice, relatedForm);

        Map<String,Object> customer = parseJsonObject(inv.getCustomer());
        String buyerName = deriveBuyerName(customer);
        String buyerFullName = str(customer.get("buyer"));
        String buyerMst = firstText(customer.get("taxcode"), customer.get("tax_code"), customer.get("taxCode"));
        String buyerAddr = str(customer.get("address"));
        String buyerCode = str(customer.get("code"));
        String buyerPhone = str(customer.get("phone"));
        String buyerEmail = str(customer.get("email"));
        String buyerBankNo = str(customer.get("bank_no"));
        String buyerBankName = str(customer.get("bank_name"));

        java.util.List<Map<String,Object>> details = parseJsonArray(inv.getDetail());
        boolean oneVatRateForm = form != null && form.getType() != null && form.getType() == 1;
        String invoiceVatRate = formatInvoiceVatRate(inv, details);

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
          .append(tag("NLap", nlap));
        if (salesInvoice) {
            sb.append(tag("HDDCKPTQuan", "0"));
        } else {
            sb.append(tag("HDCTTChinh", hdcttChinh));
        }
        sb
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
          .append(relatedXml.toXml())
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
          String tlckhau = firstText(d.get("discountRate"), d.get("discount_rate"), d.get("TLCKhau"));
          String stckhau = firstText(d.get("discountAmount"), d.get("discount_amount"), d.get("STCKhau"));
          String total = toStringOrDefault(d.get("total"), "");
          String vatRate = oneVatRateForm ? invoiceVatRate : formatVatRate(d.get("vatRate"), d.get("vatRateOther"));
          String vatAmount = toStringOrDefault(d.get("vatAmount"), "");
          String amount = toStringOrDefault(d.get("amount"), total);
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
            .append(tag("ThTien", total));
          if (!salesInvoice) {
              sb.append(tag("TSuat", vatRate))
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
                .append("</TTKhac>");
          } else {
              sb.append("<TTKhac>")
                .append("<TTin>")
                .append(tag("TTruong", "TCong"))
                .append(tag("KDLieu", "numeric"))
                .append(tag("DLieu", amount))
                .append("</TTin>")
                .append("</TTKhac>");
          }
          sb.append("</HHDVu>");
        }
        sb.append("</DSHHDVu>");

        sb.append("<TToan>");
        if (!salesInvoice) {
            Map<String, Totals> totalsByRate = new LinkedHashMap<>();
            if (oneVatRateForm) {
                Totals t = totalsByRate.computeIfAbsent(invoiceVatRate, k -> new Totals());
                t.thTien = inv.getTotal() != null ? inv.getTotal() : 0d;
                t.tThue = inv.getVatAmount() != null ? inv.getVatAmount() : 0d;
            } else {
                for (Map<String,Object> d : details) {
                    String rateKey = formatVatRate(d.get("vatRate"), d.get("vatRateOther"));
                    Totals t = totalsByRate.computeIfAbsent(rateKey, k -> new Totals());
                    t.thTien += toDouble(d.get("total"));
                    t.tThue += toDouble(d.get("vatAmount"));
                }
            }
            sb.append("<THTTLTSuat>");
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
              .append(tag("TgTThue", trimZeros(inv.getVatAmount())));
        }
        sb.append("<DSLPhi>")
          .append("</DSLPhi>")
          .append(tag("TTCKTMai", trimZeros(inv.getDiscountAmount())))
          .append(tag("TGTKhac", "0"))
          .append(tag("TgTTTBSo", trimZeros(inv.getAmount())))
          .append(tag("TgTTTBChu", nullToEmpty(inv.getAmountInWords())))
          .append(tag("TTKhac", ""))
          .append("</TToan>");
        sb.append("</NDHDon>");
        sb.append(additionalInformation(inv));
        sb.append("</DLHDon>");
        sb.append(tag("MCCQT", nullToEmpty(inv.getCodeCqt())));
        sb.append("<DLQRCode/>");
        sb.append("<DSCKS><NBan/><NMua/><CCKSKhac/></DSCKS>");
        sb.append("</HDon>");
        return sb.toString();
    }

    private static String buildStockIssue(InvoiceEntity inv, FormInvoiceEntity form, CompanyEntity company, CompanyBankEntity bank,
                                          InvoiceEntity relatedInvoice, FormInvoiceEntity relatedForm) {
        Map<String, Object> info = parseJsonObject(inv.getOthers());
        Map<String, Object> customer = parseJsonObject(inv.getCustomer());
        List<Map<String, Object>> details = parseJsonArray(inv.getDetail());
        String id = hasText(inv.getIdAttr()) ? inv.getIdAttr() : UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String sellerName = firstText(info.get("sellerName"), info.get("seller_name"), company == null ? "" : company.getName());
        String sellerTax = firstText(info.get("sellerTaxCode"), info.get("sellerTaxcode"), info.get("seller_taxcode"), company == null ? "" : company.getTaxcode());
        String sellerAddress = firstText(info.get("sellerAddress"), info.get("seller_address"), company == null ? "" : company.getAddress());
        String buyerName = firstText(info.get("buyerName"), info.get("buyer_name"), customer.get("name"), customer.get("buyer"));
        String buyerTax = firstText(info.get("buyerTaxCode"), info.get("buyerTaxcode"), customer.get("taxcode"), customer.get("taxCode"));
        String buyerAddress = firstText(info.get("buyerAddress"), info.get("buyer_address"), customer.get("address"));
        String receiver = firstText(info.get("receiverName"), info.get("receiver_name"), customer.get("buyer"));
        String sender = firstText(info.get("senderName"), info.get("sender_name"), info.get("warehouseKeeper"), info.get("warehouse_keeper"));
        String transport = firstText(info.get("transportName"), info.get("transport_name"));
        String vehicle = firstText(info.get("vehicle"), info.get("vehicleNumber"), info.get("vehicle_number"));
        String contractNo = form.getStockIssueType() != null && form.getStockIssueType() == 2
                ? firstText(info.get("economicContractNo"), info.get("economic_contract_no"))
                : firstText(info.get("contractNo"), info.get("contract_no"));
        String contractDate = firstText(info.get("economicContractDate"), info.get("economic_contract_date"));
        String dispatchNo = firstText(info.get("dispatchOrderNo"), info.get("dispatch_order_no"), info.get("lddnbo"));
        String transportContract = firstText(info.get("transportContractNo"), info.get("transport_contract_no"));
        String tieuDe = invoiceTitle(inv, form);
        RelatedInvoiceXml relatedXml = buildRelatedInvoiceXml(inv, relatedInvoice, relatedForm);
        StringBuilder sb = new StringBuilder("<HDon><DLHDon Id=\"").append(escapeXml(id)).append("\"><TTChung>");
        sb.append(tag("PBan", "2.1.1")).append(tag("THDon", tieuDe)).append(tag("KHMSHDon", "6"))
                .append(tag("KHHDon", nullToEmpty(form.getSerial()))).append(tag("SHDon", inv.getNo() == null ? "0" : String.valueOf(inv.getNo())))
                .append(tag("NLap", inv.getDateExport() == null ? "" : inv.getDateExport().toString()))
                .append(tag("HDDCKPTQuan", "0"))
                .append(tag("DVTTe", nullToEmpty(inv.getCurrency()))).append(tag("TGia", inv.getExchangeRate() == null ? "" : trimZeros(inv.getExchangeRate())))
                .append(tag("HTTToan", paymentTypeLabel(inv.getPaymentType())))
                .append(tag("MSTTCGP", company == null ? "" : nullToEmpty(company.getTaxcode()))).append(tag("TTKhac", ""))
                .append(relatedXml.toXml());
        sb.append("</TTChung><NDHDon><NBan>").append(tag("Ten", sellerName)).append(tag("MST", sellerTax)).append(tag("DChi", sellerAddress));
        if (form.getStockIssueType() != null && form.getStockIssueType() == 1) {
            sb.append(tag("LDDNBo", dispatchNo));
        } else {
            sb.append(tag("HDKTSo", contractNo)).append(tag("HDKTNgay", contractDate));
        }
        sb.append(tag("PTVChuyen", hasText(vehicle) ? vehicle : transport)).append(tag("TNVChuyen", transport)).append(tag("HVTNXHang", sender)).append(tag("HDSo", transportContract)).append(tag("TTKhac", ""));
        sb.append("</NBan><NMua>").append(tag("Ten", buyerName)).append(tag("MST", buyerTax)).append(tag("DChi", buyerAddress)).append(tag("HVTNNHang", receiver)).append(tag("TTKhac", ""));
        sb.append("</NMua><DSHHDVu>");
        int idx = 0;
        for (Map<String, Object> d : details) {
            idx++;
            String quantityExport = firstText(d.get("quantityExport"), d.get("quantity"), d.get("qty"));
            String quantityImport = firstText(d.get("quantityImport"), d.get("quantityIn"), d.get("actualImport"), 0);
            String lineTotal = firstText(d.get("total"), d.get("thanhTien"), d.get("amount"));
            String amount = firstText(d.get("amount"), d.get("total"), d.get("thanhTien"));
            sb.append("<HHDVu>").append(tag("TChat", firstText(d.get("feature"), d.get("tchat"))))
                    .append(tag("STT", firstText(d.get("num"), idx))).append(tag("MHHDVu", firstText(d.get("code"), d.get("itemCode"))))
                    .append(tag("THHDVu", firstText(d.get("name"), d.get("itemName")))).append(tag("DVTinh", firstText(d.get("unit"), d.get("unitName"))))
                    .append(tag("SLuong", quantityExport)).append(tag("DGia", firstText(d.get("price"), d.get("unitPrice")))).append(tag("ThTien", lineTotal))
                    .append(stockOtherXml(amount, quantityExport, quantityImport,
                            Integer.valueOf(1).equals(form.getStockIssueType())));
            sb.append("</HHDVu>");
        }
        sb.append("</DSHHDVu><TToan>").append(tag("TgTTTBSo", trimZeros(inv.getAmount() == null ? inv.getTotal() : inv.getAmount())))
                .append(tag("TgTTTBChu", nullToEmpty(inv.getAmountInWords()))).append(tag("TTKhac", ""))
                .append("</TToan></NDHDon>").append(additionalInformation(inv)).append("</DLHDon>")
                .append(tag("MCCQT", nullToEmpty(inv.getCodeCqt()))).append("<DLQRCode/><DSCKS><NBan/><NMua/><CCKSKhac/></DSCKS></HDon>");
        return sb.toString();
    }

    private static String stockOtherXml(String amount, String quantityExport, String quantityImport,
                                        boolean includeActualQuantities) {
        StringBuilder xml = new StringBuilder("<TTKhac>")
                .append("<TTin>").append(tag("TTruong", "TCong")).append(tag("KDLieu", "numeric"))
                .append(tag("DLieu", amount)).append("</TTin>");
        if (includeActualQuantities) {
            xml.append("<TTin>").append(tag("TTruong", "SLuongThucXuat")).append(tag("KDLieu", "numeric"))
                    .append(tag("DLieu", quantityExport)).append("</TTin>")
                    .append("<TTin>").append(tag("TTruong", "SLuongThucNhap")).append(tag("KDLieu", "numeric"))
                    .append(tag("DLieu", quantityImport)).append("</TTin>");
        }
        return xml.append("</TTKhac>").toString();
    }

    private static RelatedInvoiceXml buildRelatedInvoiceXml(InvoiceEntity inv, InvoiceEntity relatedInvoice, FormInvoiceEntity relatedForm) {
        RelatedInvoiceXml xml = new RelatedInvoiceXml();
        short invoiceType = inv != null && inv.getInvoiceType() != null ? inv.getInvoiceType() : 0;
        if ((invoiceType != 1 && invoiceType != 2) || relatedInvoice == null) {
            return xml;
        }
        xml.tchDon = String.valueOf(invoiceType);
        xml.lhdcLQuan = relatedForm != null && relatedForm.getCategory() != null ? String.valueOf(relatedForm.getCategory()) : "1";
        xml.khmsHdcLQuan = relatedForm != null ? nullToEmpty(relatedForm.getFormCode()) : "";
        xml.khhdcLQuan = relatedForm != null ? nullToEmpty(relatedForm.getSerial()) : "";
        xml.shdcLQuan = relatedInvoice.getNo() != null ? String.valueOf(relatedInvoice.getNo()) : "";
        xml.nlhdcLQuan = relatedInvoice.getDateExport() != null
                ? relatedInvoice.getDateExport().toString()
                : "";
        // Lý do điều chỉnh/thay thế theo Phụ lục XVII.
        xml.lddcTThe = relatedReason(inv.getRelated());
        xml.gChu = buildRelatedNote(invoiceType, inv.getInvoiceTypeAdjust(), relatedInvoice, relatedForm);
        return xml;
    }

    private static String relatedReason(String value) {
        if (value != null && value.trim().matches("\\d{1,2}")) {
            int code = Integer.parseInt(value.trim());
            if (code >= 1 && code <= 14) return String.valueOf(code);
        }
        return "";
    }

    private static String buildRelatedNote(short invoiceType, Short adjustType, InvoiceEntity relatedInvoice, FormInvoiceEntity relatedForm) {
        String action = invoiceType == 1 ? "thay thế" : "điều chỉnh";
        String adjustSuffix = "";
        if (invoiceType == 2) {
            adjustSuffix = switch (adjustType == null ? 0 : adjustType) {
                case 1 -> " tăng";
                case 2 -> " giảm";
                case 3 -> " thông tin";
                default -> "";
            };
        }
        String no = relatedInvoice != null && relatedInvoice.getNo() != null ? String.valueOf(relatedInvoice.getNo()) : "";
        String formCode = relatedForm != null ? nullToEmpty(relatedForm.getFormCode()) : "";
        String serial = relatedForm != null ? nullToEmpty(relatedForm.getSerial()) : "";
        String dateText = "";
        if (relatedInvoice != null && relatedInvoice.getDateExport() != null) {
            dateText = "ngày " + relatedInvoice.getDateExport().format(DateTimeFormatter.ofPattern("dd 'tháng' MM 'năm' yyyy"));
        }
        return "Là hóa đơn " + action + adjustSuffix + " cho hóa đơn số " + no
                + ", mẫu số " + formCode + ", ký hiệu " + serial
                + (hasText(dateText) ? ", " + dateText : "");
    }

    private static final class RelatedInvoiceXml {
        String tchDon = "";
        String lhdcLQuan = "";
        String khmsHdcLQuan = "";
        String khhdcLQuan = "";
        String shdcLQuan = "";
        String nlhdcLQuan = "";
        String lddcTThe = "";
        String gChu = "";

        String toXml() {
            return new StringBuilder()
                    .append("<TTHDLQuan>")
                    .append(tag("TCHDon", tchDon))
                    .append(tag("LDDCTThe", lddcTThe))
                    .append(tag("LHDCLQuan", lhdcLQuan))
                    .append(tag("KHMSHDCLQuan", khmsHdcLQuan))
                    .append(tag("KHHDCLQuan", khhdcLQuan))
                    .append(tag("SHDCLQuan", shdcLQuan))
                    .append(tag("NLHDCLQuan", nlhdcLQuan))
                    .append(tag("GChu", gChu))
                    .append(tag("SBKCLQuan", ""))
                    .append(tag("NBKCLQuan", ""))
                    .append("</TTHDLQuan>")
                    .toString();
        }
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
    private static String invoiceTitle(InvoiceEntity invoice, FormInvoiceEntity form) {
        if (form != null && hasText(form.getName())) return form.getName().trim();
        return nullToEmpty(invoice.getName());
    }
    private static boolean hasText(String s) { return s != null && !s.trim().isEmpty(); }

    private static String tag(String name, String value) {
        return "<" + name + ">" + escapeXml(value) + "</" + name + ">";
    }

    /**
     * Mã tra cứu là thông tin mở rộng của chứng từ, không phải một trường
     * trong TTChung.  Vì vậy nó phải nằm tại HDon/DLHDon/TTKhac.
     */
    private static String additionalInformation(InvoiceEntity invoice) {
        return new StringBuilder("<TTKhac><TTin>")
                .append(tag("TTruong", "MTCuu"))
                .append(tag("KDLieu", "string"))
                .append(tag("DLieu", nullToEmpty(invoice.getLookupCode())))
                .append("</TTin></TTKhac>")
                .toString();
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
    private static String firstText(Object... values) {
        if (values == null) return "";
        for (Object value : values) {
            String s = str(value);
            if (hasText(s)) return s;
        }
        return "";
    }
    private static String str(Object o) { return o == null ? "" : String.valueOf(o); }
    private static String toStringOrDefault(Object o, String def) { String s = str(o); return hasText(s) ? s : def; }
    private static String formatInvoiceVatRate(InvoiceEntity inv, java.util.List<Map<String,Object>> details) {
        String invoiceRate = inv != null ? formatVatRate(inv.getVatRate(), inv.getVatRateOther()) : "";
        String detailRate = firstDetailVatRate(details);
        if (hasText(invoiceRate) && (!"KCT".equals(invoiceRate) || !hasText(detailRate))) {
            return invoiceRate;
        }
        return hasText(detailRate) ? detailRate : invoiceRate;
    }

    private static String firstDetailVatRate(java.util.List<Map<String,Object>> details) {
        if (details == null) return "";
        for (Map<String,Object> detail : details) {
            if (detail == null) continue;
            String rate = formatVatRate(detail.get("vatRate"), detail.get("vatRateOther"));
            if (hasText(rate)) return rate;
        }
        return "";
    }

    private static String formatVatRate(Object rateObj) {
        return formatVatRate(rateObj, null);
    }

    private static String formatVatRate(Object rateObj, Object otherRateObj) {
        if (rateObj == null) return "";
        try {
            int r = Integer.parseInt(String.valueOf(rateObj));
            return switch (r) {
                case -1 -> "KCT";
                case -3 -> formatOtherVatRate(otherRateObj);
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

    private static String formatOtherVatRate(Object otherRateObj) {
        if (otherRateObj == null) return "";
        String raw = String.valueOf(otherRateObj).trim();
        if (raw.isEmpty()) return "";
        if (raw.endsWith("%")) return raw;
        try {
            double value = Double.parseDouble(raw);
            return trimZeros(value) + "%";
        } catch (Exception ex) {
            return raw;
        }
    }
    private static double toDouble(Object o) {
        if (o == null) return 0d;
        try { return Double.parseDouble(String.valueOf(o)); } catch (Exception ex) { return 0d; }
    }
    private static class Totals { double thTien = 0d; double tThue = 0d; }
}
