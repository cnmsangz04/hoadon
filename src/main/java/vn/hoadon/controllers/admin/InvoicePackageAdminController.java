package vn.hoadon.controllers.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.common.IdRequestDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageFilterDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackagePurchaseDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackagePurchaseFilterDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageRequestDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageResponseDTO;
import vn.hoadon.dto.invoicepackage.InvoicePackageStatisticsDTO;
import vn.hoadon.services.InvoicePackageService;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/v1/administrator/invoice-packages")
public class InvoicePackageAdminController extends BaseController {

    @Autowired
    private InvoicePackageService service;

    @PostMapping("/list")
    public ResponseEntity<Page<InvoicePackageResponseDTO>> listPackages(
            @RequestBody(required = false) InvoicePackageFilterDTO filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        permission("buy-invoice-list");
        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending().and(Sort.by("id").descending()));
        return ResponseEntity.ok(service.listPackages(filter, pageable));
    }

    @PostMapping("/save")
    public ResponseEntity<InvoicePackageResponseDTO> savePackage(@RequestBody InvoicePackageRequestDTO dto) {
        permission("buy-invoice-save");
        return ResponseEntity.ok(service.savePackage(dto));
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deletePackage(@RequestBody IdRequestDTO dto) {
        permission("buy-invoice-delete");
        service.deletePackage(dto.getId());
        return ResponseEntity.ok(Map.of("message", "Đã ngưng hoạt động gói hóa đơn"));
    }

    @PostMapping("/purchases")
    public ResponseEntity<Page<InvoicePackagePurchaseDTO>> listPurchases(
            @RequestBody(required = false) InvoicePackagePurchaseFilterDTO filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        permission("buy-invoice-list");
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(service.listPurchases(filter, pageable));
    }

    @PostMapping("/statistics")
    public ResponseEntity<InvoicePackageStatisticsDTO> statistics(
            @RequestBody(required = false) InvoicePackagePurchaseFilterDTO filter
    ) {
        permission("buy-invoice-list");
        return ResponseEntity.ok(service.statistics(filter));
    }

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody(required = false) InvoicePackagePurchaseFilterDTO filter) {
        permission("buy-invoice-list");
        byte[] bytes = service.exportPurchases(filter);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("bao-cao-mua-goi-hoa-don.xlsx", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }
}
