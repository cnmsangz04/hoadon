package vn.hoadon.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceEntity;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.InvoiceRepository;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/public/invoices")
public class PublicInvoiceLookupController {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final InvoiceRepository invoiceRepository;
    private final CompanyRepository companyRepository;
    private final FormInvoiceRepository formInvoiceRepository;

    public PublicInvoiceLookupController(InvoiceRepository invoiceRepository,
                                         CompanyRepository companyRepository,
                                         FormInvoiceRepository formInvoiceRepository) {
        this.invoiceRepository = invoiceRepository;
        this.companyRepository = companyRepository;
        this.formInvoiceRepository = formInvoiceRepository;
    }

    @GetMapping("/lookup")
    public ResponseEntity<?> lookup(@RequestParam(name = "code", required = false) String code,
                                    @RequestParam(name = "taxcode", required = false) String taxcode) {
        String lookupCode = trim(code);
        String sellerTaxcode = trim(taxcode);
        if (lookupCode == null || sellerTaxcode == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng nhập mã tra cứu và mã số thuế"));
        }

        InvoiceEntity invoice = invoiceRepository.findByLookupCode(lookupCode).orElse(null);
        if (invoice == null) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy hóa đơn"));
        }

        CompanyEntity company = companyRepository.findByTaxcode(sellerTaxcode).orElse(null);
        Long invoiceCompanyId = invoice.getCompanyId() != null ? invoice.getCompanyId().longValue() : null;
        if (company == null || invoiceCompanyId == null || !company.getId().equals(invoiceCompanyId)) {
            return ResponseEntity.status(404).body(Map.of("message", "Không tìm thấy hóa đơn phù hợp với mã số thuế"));
        }

        FormInvoiceEntity form = invoice.getFormId() != null
                ? formInvoiceRepository.findById(invoice.getFormId().longValue()).orElse(null)
                : null;

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lookupCode", invoice.getLookupCode());
        data.put("invoiceNo", invoice.getNo());
        data.put("invoiceCode", invoice.getCode());
        data.put("codeCqt", invoice.getCodeCqt());
        data.put("dateExport", invoice.getDateExport());
        data.put("amount", invoice.getAmount());
        data.put("currency", invoice.getCurrency());
        data.put("status", invoice.getStatus());
        data.put("statusText", statusText(invoice.getStatus()));
        data.put("companyName", company.getName());
        data.put("companyTaxcode", company.getTaxcode());
        data.put("companyAddress", company.getAddress());
        data.put("customerName", readCustomerName(invoice.getCustomer()));
        data.put("formCode", form != null ? form.getFormCode() : null);
        data.put("serial", form != null ? form.getSerial() : null);
        data.put("viewUrl", "/v1/invoices/" + lookupCode + "/view");
        data.put("pdfUrl", "/v1/invoices/" + lookupCode + "/download-pdf");
        data.put("xmlUrl", "/v1/invoices/" + lookupCode + "/download-xml");
        return ResponseEntity.ok(data);
    }

    private String trim(String value) {
        if (value == null) return null;
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private String readCustomerName(String customerJson) {
        if (customerJson == null || customerJson.isBlank()) return null;
        try {
            Map<String, Object> map = JSON.readValue(customerJson, new TypeReference<>() {});
            Object value = firstValue(map, "name", "buyer_name", "customer_name", "cus_name");
            return value != null ? String.valueOf(value) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object firstValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);
            if (value != null && !String.valueOf(value).isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String statusText(Short status) {
        if (status == null) return "Không xác định";
        return switch (status) {
            case 0 -> "Mới khởi tạo";
            case 1 -> "Đã ký";
            case 2 -> "Đã gửi thuế";
            case 3 -> "Đã phát hành";
            case 4 -> "Đã hủy";
            case 5 -> "Đã thay thế";
            case 6 -> "Đã điều chỉnh";
            case 7 -> "Không đủ điều kiện cấp mã";
            default -> "Không xác định";
        };
    }
}
