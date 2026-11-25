package vn.hoadon.invoice.template.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import vn.hoadon.entity.*;
import vn.hoadon.util.InvoiceXmlBuilder;

import java.time.LocalDate;
import java.util.*;

@Component
public class InvoiceFixtureFactory {
    private final ObjectMapper mapper;
    public InvoiceFixtureFactory(ObjectMapper mapper) { this.mapper = mapper; }

    public String sample(FormInvoiceEntity form, CompanyEntity company, CompanyBankEntity bank, int rows) {
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setIdAttr("00000000000000000000000000000001");
        boolean stockIssue = Objects.equals(form.getCategory(), 6);
        invoice.setName(stockIssue
                ? (Objects.equals(form.getStockIssueType(), 2) ? "PHIẾU XUẤT KHO HÀNG GỬI BÁN ĐẠI LÝ" : "PHIẾU XUẤT KHO KIÊM VẬN CHUYỂN NỘI BỘ")
                : Objects.equals(form.getCategory(), 2) ? "HÓA ĐƠN BÁN HÀNG" : "HÓA ĐƠN GIÁ TRỊ GIA TĂNG");
        invoice.setNo(1);
        invoice.setDateExport(LocalDate.of(2026, 1, 20));
        invoice.setCurrency("VND");
        invoice.setExchangeRate(1d);
        invoice.setPaymentType((short) 3);
        boolean salesInvoice = Objects.equals(form.getCategory(), 2) || stockIssue;
        invoice.setVatRate(salesInvoice ? null : Short.valueOf((short) 10));
        invoice.setLookupCode("SAMPLE0001");
        invoice.setCodeCqt("MCCQT-MINH-HOA");
        invoice.setAmountInWords("Số tiền bằng chữ dùng để minh họa bố cục hóa đơn.");
        invoice.setDiscountAmount(0d);
        invoice.setCustomer(json(Map.of("name", "Công ty khách hàng minh họa", "taxcode", "0100000000", "address", "Số 10, đường Nguyễn Huệ, Thành phố Hồ Chí Minh", "buyer", "Nguyễn Văn An")));
        if (stockIssue) {
            Map<String, Object> stockInfo = new LinkedHashMap<>();
            stockInfo.put("buyerAddress", "Số 10, đường Nguyễn Huệ, Thành phố Hồ Chí Minh");
            stockInfo.put("receiverName", "Nguyễn Văn An");
            stockInfo.put("senderName", "Trần Văn Cường");
            stockInfo.put("transportName", "Nguyễn Văn Bình");
            stockInfo.put("vehicle", "29C-123.45");
            if (Objects.equals(form.getStockIssueType(), 2)) {
                stockInfo.put("economicContractNo", "HĐKT-MINH-HỌA");
                stockInfo.put("economicContractDate", "2026-01-20");
            } else {
                stockInfo.put("dispatchOrderNo", "LDDN-MINH-HỌA");
            }
            invoice.setOthers(json(stockInfo));
        }
        List<Map<String, Object>> details = new ArrayList<>();
        boolean multiRate = Objects.equals(form.getType(), 2);
        double total = 0d;
        double vatAmount = 0d;
        int[] sampleRates = {5, 10, 8};
        for (int i = 1; i <= rows; i++) {
            int vatRate = salesInvoice ? 0 : (multiRate ? sampleRates[(i - 1) % sampleRates.length] : 10);
            double lineTotal = 100000d;
            double lineVat = salesInvoice ? 0d : lineTotal * vatRate / 100d;
            total += lineTotal;
            vatAmount += lineVat;
            details.add(Map.of("name", i == 2 ? "Dịch vụ có nội dung mô tả dài để kiểm tra tự xuống dòng, giữ nguyên tiếng Việt và không mất dữ liệu khi phân trang hóa đơn" : "Hàng hóa, dịch vụ minh họa " + i, "unit", "Lần", "num", i, "quantity", 1, "feature", 1, "price", lineTotal, "total", lineTotal, "vatAmount", lineVat, "amount", lineTotal + lineVat, "vatRate", vatRate));
        }
        invoice.setTotal(total);
        invoice.setVatAmount(salesInvoice ? 0d : vatAmount);
        invoice.setAmount(salesInvoice ? total : total + vatAmount);
        invoice.setDetail(json(details));
        if (company == null) {
            company = new CompanyEntity();
            company.setName("CÔNG TY MINH HỌA");
            company.setTaxcode("0100000000");
            company.setAddress("Số 1, đường Trần Hưng Đạo, Hà Nội");
        }
        return InvoiceXmlBuilder.build(invoice, form, company, bank);
    }

    public String sample(int category, int rows) {
        return sample(category, category == 2 ? -1 : category == 6 ? 0 : 1, rows);
    }

    public String sample(int category, int type, int rows) {
        FormInvoiceEntity form = new FormInvoiceEntity();
        form.setCategory(category);
        form.setType(type);
        form.setFormCode(String.valueOf(category));
        if (category == 6) {
            form.setStockIssueType(1);
            form.setSerial("C26NAA");
        } else {
            form.setSerial("C26TAA");
        }
        return sample(form, null, null, rows);
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
