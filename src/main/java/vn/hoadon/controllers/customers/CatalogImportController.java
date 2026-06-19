package vn.hoadon.controllers.customers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.invoiceimport.InvoiceImportDTO;
import vn.hoadon.dto.invoiceimport.InvoiceImportResultDTO;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.services.CatalogImportService;

@RestController
@RequestMapping("/v1/catalog-imports")
public class CatalogImportController extends BaseController {

    @Autowired
    private CatalogImportService service;

    @GetMapping("/{type}/template")
    public ResponseEntity<byte[]> template(@PathVariable("type") String type) {
        permission(savePermission(type));
        return service.template(type);
    }

    @GetMapping("/{type}")
    public ResponseEntity<PageDTO<InvoiceImportDTO>> list(
            @PathVariable("type") String type,
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        permission(listPermission(type) + "|" + savePermission(type));
        int pageIndex = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, Math.max(size, 1));
        Page<InvoiceImportDTO> result = service.list(type, user, pageable);
        return ResponseEntity.ok(new PageDTO<>(result.getContent(), result.getNumber() + 1, result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @PostMapping("/{type}/upload")
    public ResponseEntity<InvoiceImportResultDTO> upload(
            @PathVariable("type") String type,
            @AuthenticationPrincipal UserEntity user,
            @RequestParam("file") MultipartFile file
    ) {
        permission(savePermission(type));
        return ResponseEntity.ok(service.importFile(type, file, user));
    }

    @PostMapping("/{type}/{id}/reimport")
    public ResponseEntity<InvoiceImportResultDTO> reimport(
            @PathVariable("type") String type,
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("id") Long id
    ) {
        permission(savePermission(type));
        return ResponseEntity.ok(service.reimport(type, id, user));
    }

    private String listPermission(String type) {
        String normalized = normalizeType(type);
        if ("customer".equals(normalized)) return "category-customer-list|import-customer-list";
        if ("product".equals(normalized)) return "category-product-list|import-product-list";
        throw new IllegalArgumentException("Loại import không hợp lệ");
    }

    private String savePermission(String type) {
        String normalized = normalizeType(type);
        if ("customer".equals(normalized)) return "category-customer-save|import-customer-save";
        if ("product".equals(normalized)) return "category-product-save|import-product-save";
        throw new IllegalArgumentException("Loại import không hợp lệ");
    }

    private String normalizeType(String type) {
        return type == null ? "" : type.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public static class PageDTO<T> {
        public java.util.List<T> items;
        public int current_page;
        public int per_page;
        public long total;
        public int last_page;

        public PageDTO(java.util.List<T> items, int currentPage, int perPage, long total, int lastPage) {
            this.items = items;
            this.current_page = currentPage;
            this.per_page = perPage;
            this.total = total;
            this.last_page = lastPage;
        }
    }
}
