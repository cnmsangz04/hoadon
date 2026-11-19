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
        invoice.setName(Objects.equals(form.getCategory(), 2) ? "HÓA ĐƠN BÁN HÀNG" : "HÓA ĐƠN GIÁ TRỊ GIA TĂNG");
        invoice.setNo(1);
        invoice.setDateExport(LocalDate.of(2026, 1, 20));
        invoice.setCurrency("VND");
        invoice.setExchangeRate(1d);
        invoice.setPaymentType((short) 3);
        invoice.setVatRate((short) 10);
        invoice.setLookupCode("SAMPLE0001");
        invoice.setCodeCqt("MCCQT-MINH-HOA");
        invoice.setAmountInWords("Số tiền bằng chữ dùng để minh họa bố cục hóa đơn.");
        invoice.setDiscountAmount(0d);
        invoice.setCustomer(json(Map.of("name", "Công ty khách hàng minh họa", "taxcode", "0100000000", "address", "Số 10, đường Nguyễn Huệ, Thành phố Hồ Chí Minh", "buyer", "Nguyễn Văn An")));
        List<Map<String, Object>> details = new ArrayList<>();
        boolean multiRate = Objects.equals(form.getType(), 2);
        double total = 0d;
        double vatAmount = 0d;
        int[] sampleRates = {5, 10, 8};
        for (int i = 1; i <= rows; i++) {
            int vatRate = multiRate ? sampleRates[(i - 1) % sampleRates.length] : 10;
            double lineTotal = 100000d;
            double lineVat = lineTotal * vatRate / 100d;
            total += lineTotal;
            vatAmount += lineVat;
            details.add(Map.of("name", i == 2 ? "Dịch vụ có nội dung mô tả dài để kiểm tra tự xuống dòng, giữ nguyên tiếng Việt và không mất dữ liệu khi phân trang hóa đơn" : "Hàng hóa, dịch vụ minh họa " + i, "unit", "Lần", "num", i, "quantity", 1, "feature", 1, "price", lineTotal, "total", lineTotal, "vatAmount", lineVat, "amount", lineTotal + lineVat, "vatRate", vatRate));
        }
        invoice.setTotal(total);
        invoice.setVatAmount(vatAmount);
        invoice.setAmount(total + vatAmount);
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
        return sample(category, 1, rows);
    }

    public String sample(int category, int type, int rows) {
        FormInvoiceEntity form = new FormInvoiceEntity();
        form.setCategory(category);
        form.setType(type);
        form.setFormCode(String.valueOf(category));
        form.setSerial("C26TAA");
        return sample(form, null, null, rows);
    }

    private String json(Object value) {
        try { return mapper.writeValueAsString(value); }
        catch (Exception ex) { throw new IllegalStateException(ex); }
    }
}
