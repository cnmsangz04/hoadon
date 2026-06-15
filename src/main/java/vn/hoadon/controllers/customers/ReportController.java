package vn.hoadon.controllers.customers;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.dto.InvoiceDTO;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.InvoiceRepository;
import vn.hoadon.services.InvoiceService;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/reports")
public class ReportController extends BaseController {

    @Autowired
    private InvoiceRepository invoiceRepository;

    @Autowired
    private FormInvoiceRepository formInvoiceRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping("/invoices")
    public ResponseEntity<?> listInvoices(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(name = "category", required = false) Integer category,
            @RequestParam(name = "status", required = false) Short status,
            @RequestParam(name = "periodType", defaultValue = "month") String periodType,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        permission("report-invoice");
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().body(error("Không xác định được công ty/người dùng"));
        }
        Long companyId = user.getCompanyId();
        int pageIndex = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.DESC, "id"));

        DateRange range = buildDateRange(periodType, month, quarter, year, fromDate, toDate);

        Page<InvoiceEntity> pageData = invoiceRepository.searchReportInvoices(
                companyId,
                category,
                status,
                range.from,
                range.to,
                pageable
        );

        // Ánh xạ ra DTO cho phù hợp với list.vue
        List<Map<String, Object>> items = new ArrayList<>();
        Map<Long, FormInvoiceEntity> formCache = new HashMap<>();
        for (InvoiceEntity inv : pageData.getContent()) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", inv.getId());
            row.put("no", inv.getNo());
            row.put("dateExport", inv.getDateExport());
            row.put("paymentType", inv.getPaymentType());
            row.put("status", inv.getStatus());
            row.put("amount", inv.getAmount());
            row.put("orderCode", inv.getLookupCode());
            // Mã hóa đơn nếu có cột bill.code trong JSON others hoặc bill; ở đây dùng lookupCode làm mã đơn hàng

            Long formId = inv.getFormId() != null ? inv.getFormId().longValue() : null;
            if (formId != null) {
                FormInvoiceEntity form = formCache.computeIfAbsent(formId, id -> formInvoiceRepository.findById(id).orElse(null));
                if (form != null) {
                    row.put("formCode", form.getFormCode());
                    row.put("serial", form.getSerial());
                }
            }

            // Tên đơn vị / khách hàng: customer.name ưu tiên, sau đó customer.buyer
            String customerName = extractCustomerName(inv.getCustomer());
            row.put("customerName", customerName);

            items.add(row);
        }

        PageDTO<Map<String, Object>> dto = new PageDTO<>(
                items,
                pageData.getNumber() + 1,
                pageData.getSize(),
                pageData.getTotalElements(),
                pageData.getTotalPages()
        );
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/invoices/export")
    public ResponseEntity<?> exportInvoices(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(name = "category", required = false) Integer category,
            @RequestParam(name = "status", required = false) Short status,
            @RequestParam(name = "periodType", defaultValue = "month") String periodType,
            @RequestParam(name = "month", required = false) Integer month,
            @RequestParam(name = "quarter", required = false) Integer quarter,
            @RequestParam(name = "year", required = false) Integer year,
            @RequestParam(name = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(name = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        permission("report-invoice-export");
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().body(error("Không xác định được công ty/người dùng"));
        }
        Long companyId = user.getCompanyId();

        DateRange range = buildDateRange(periodType, month, quarter, year, fromDate, toDate);

        // Lấy toàn bộ dữ liệu trong phạm vi (limit an toàn 20k bản ghi)
        Pageable pageable = PageRequest.of(0, 20000, Sort.by(Sort.Direction.DESC, "id"));
        Page<InvoiceEntity> pageData = invoiceRepository.searchReportInvoices(
                companyId,
                category,
                status,
                range.from,
                range.to,
                pageable
        );

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Bao_cao_hoa_don");
            int rowIdx = 0;

            // Kiểu tiêu đề
            CellStyle headerStyle = wb.createCellStyle();
            Font headerFont = wb.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // Dòng tiêu đề
            Row header = sheet.createRow(rowIdx++);
            String[] cols = new String[] {
                    "STT",
                    "Mã đơn hàng",
                    "Ký hiệu",
                    "Số hóa đơn",
                    "Ngày lập",
                    "Hình thức thanh toán",
                    "Trạng thái",
                    "Mã khách hàng",
                    "Tên đơn vị",
                    "Tên khách hàng",
                    "Mã số thuế",
                    "Địa chỉ",
                    "Tổng tiền hàng",
                    "Tổng tiền thuế",
                    "Tổng tiền thanh toán"
            };
            for (int i = 0; i < cols.length; i++) {
                Cell c = header.createCell(i);
                c.setCellValue(cols[i]);
                c.setCellStyle(headerStyle);
            }

            Map<Long, FormInvoiceEntity> formCache = new HashMap<>();

            int stt = 1;
            for (InvoiceEntity inv : pageData.getContent()) {
                Row row = sheet.createRow(rowIdx++);

                // Parse customer JSON
                CustomerInfo cust = extractCustomer(inv.getCustomer());

                int col = 0;
                row.createCell(col++).setCellValue(stt++); // STT
                // Mã đơn hàng: invoices.bill.code (nếu lưu trong others/bill). Tạm dùng lookupCode là mã đơn hàng
                row.createCell(col++).setCellValue(safe(inv.getLookupCode()));

                // Ký hiệu: form_invoices.form_code + form_invoices.serial
                String formCode = "";
                String serial = "";
                Long formId = inv.getFormId() != null ? inv.getFormId().longValue() : null;
                if (formId != null) {
                    FormInvoiceEntity form = formCache.computeIfAbsent(formId, id -> formInvoiceRepository.findById(id).orElse(null));
                    if (form != null) {
                        formCode = safe(form.getFormCode());
                        serial = safe(form.getSerial());
                    }
                }
                row.createCell(col++).setCellValue((formCode + serial).trim());

                // Số hóa đơn
                if (inv.getNo() != null) row.createCell(col++).setCellValue(inv.getNo()); else col++;

                // Ngày lập
                if (inv.getDateExport() != null) {
                    row.createCell(col++).setCellValue(inv.getDateExport().toString());
                } else {
                    col++;
                }

                // Hình thức thanh toán
                row.createCell(col++).setCellValue(paymentTypeLabel(inv.getPaymentType()));

                // Trạng thái
                row.createCell(col++).setCellValue(statusLabel(inv.getStatus()));

                // Mã khách hàng
                row.createCell(col++).setCellValue(safe(cust.code));
                // Tên đơn vị (ưu tiên customer.name)
                row.createCell(col++).setCellValue(safe(cust.name));
                // Tên khách hàng (buyer)
                row.createCell(col++).setCellValue(safe(cust.buyer));
                // Mã số thuế
                row.createCell(col++).setCellValue(safe(cust.taxcode));
                // Địa chỉ
                row.createCell(col++).setCellValue(safe(cust.address));

                // Tổng tiền hàng
                if (inv.getTotal() != null) row.createCell(col++).setCellValue(inv.getTotal()); else col++;
                // Tổng tiền thuế
                if (inv.getVatAmount() != null) row.createCell(col++).setCellValue(inv.getVatAmount()); else col++;
                // Tổng tiền thanh toán
                if (inv.getAmount() != null) row.createCell(col++).setCellValue(inv.getAmount()); else col++;
            }

            for (int i = 0; i < cols.length; i++) {
                sheet.autoSizeColumn(i);
            }

            wb.write(out);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
            String filename = "bao-cao-hoa-don-" + LocalDateTime.now().toString().replace(":", "-") + ".xlsx";
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename);
            return ResponseEntity.ok().headers(headers).body(out.toByteArray());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(error("Không thể xuất Excel báo cáo hóa đơn"));
        }
    }

    private DateRange buildDateRange(String periodType, Integer month, Integer quarter, Integer year, LocalDate from, LocalDate to) {
        LocalDate now = LocalDate.now();
        if (year == null || year <= 0) year = now.getYear();

        LocalDate fromDate;
        LocalDate toDate;
        if ("month".equalsIgnoreCase(periodType)) {
            int m = (month != null && month >= 1 && month <= 12) ? month : now.getMonthValue();
            YearMonth ym = YearMonth.of(year, m);
            fromDate = ym.atDay(1);
            toDate = ym.atEndOfMonth();
        } else if ("quarter".equalsIgnoreCase(periodType)) {
            int q = (quarter != null && quarter >= 1 && quarter <= 4) ? quarter : ((now.getMonthValue() - 1) / 3 + 1);
            int startMonth = (q - 1) * 3 + 1;
            YearMonth ymStart = YearMonth.of(year, startMonth);
            YearMonth ymEnd = YearMonth.of(year, startMonth + 2);
            fromDate = ymStart.atDay(1);
            toDate = ymEnd.atEndOfMonth();
        } else {
            // none: dùng from/to nếu có, nếu không thì không lọc theo ngày (null)
            fromDate = from;
            toDate = to;
        }
        return new DateRange(fromDate, toDate);
    }

    private static class DateRange {
        LocalDate from;
        LocalDate to;
        DateRange(LocalDate f, LocalDate t) { this.from = f; this.to = t; }
    }

    private Map<String, String> error(String msg) {
        Map<String, String> m = new HashMap<>();
        m.put("message", msg);
        return m;
    }

    // Lấy tên hiển thị từ JSON customer (ưu tiên name, sau đó buyer)
    private String extractCustomerName(String customerJson) {
        CustomerInfo c = extractCustomer(customerJson);
        if (c.name != null && !c.name.trim().isEmpty()) return c.name.trim();
        if (c.buyer != null && !c.buyer.trim().isEmpty()) return c.buyer.trim();
        return "";
    }

    private CustomerInfo extractCustomer(String customerJson) {
        CustomerInfo info = new CustomerInfo();
        if (customerJson == null || customerJson.trim().isEmpty()) return info;
        try {
            String t = customerJson.trim();
            if (t.startsWith("{") && t.endsWith("}")) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<?,?> map = mapper.readValue(t, Map.class);
                Object code = map.get("code");
                Object name = map.get("name");
                Object buyer = map.get("buyer");
                Object taxcode = map.get("taxcode");
                Object address = map.get("address");
                info.code = code != null ? String.valueOf(code) : null;
                info.name = name != null ? String.valueOf(name) : null;
                info.buyer = buyer != null ? String.valueOf(buyer) : null;
                info.taxcode = taxcode != null ? String.valueOf(taxcode) : null;
                info.address = address != null ? String.valueOf(address) : null;
            }
        } catch (Exception ignore) {}
        return info;
    }

    private static class CustomerInfo {
        String code;
        String name;
        String buyer;
        String taxcode;
        String address;
    }

    private String paymentTypeLabel(Short t) {
        if (t == null) return "";
        switch (t) {
            case 1: return "Tiền mặt";
            case 2: return "Chuyển khoản";
            case 3: return "Tiền mặt/Chuyển khoản";
            default: return "";
        }
    }

    private String statusLabel(Short s) {
        if (s == null) return "";
        switch (s) {
            case 0: return "Mới khởi tạo";
            case 1: return "Đã ký";
            case 2: return "Đã gửi thuế";
            case 3: return "Đã phát hành";
            case 4: return "Bị thay thế";
            case 5: return "Bị điều chỉnh";
            case 6: return "Đã hủy";
            case 7: return "Không đủ điều kiện";
            default: return "";
        }
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    public static class PageDTO<T> {
        public List<T> items;
        public int current_page;
        public int per_page;
        public long total;
        public int last_page;
        public PageDTO(List<T> items, int currentPage, int perPage, long total, int lastPage) {
            this.items = items; this.current_page = currentPage; this.per_page = perPage; this.total = total; this.last_page = lastPage;
        }
    }
}
