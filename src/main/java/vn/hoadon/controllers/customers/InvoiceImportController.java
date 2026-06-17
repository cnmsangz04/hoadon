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
import vn.hoadon.services.InvoiceImportService;

@RestController
@RequestMapping("/v1/invoice-imports")
public class InvoiceImportController extends BaseController {

    @Autowired
    private InvoiceImportService service;

    @GetMapping("/template")
    public ResponseEntity<byte[]> template() {
        permission("invoice-save");
        return service.template();
    }

    @GetMapping
    public ResponseEntity<PageDTO<InvoiceImportDTO>> list(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        permission("invoice-list|invoice-save");
        int pageIndex = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, Math.max(size, 1));
        Page<InvoiceImportDTO> result = service.list(user, pageable);
        return ResponseEntity.ok(new PageDTO<>(result.getContent(), result.getNumber() + 1, result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    @PostMapping("/upload")
    public ResponseEntity<InvoiceImportResultDTO> upload(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam("file") MultipartFile file
    ) {
        permission("invoice-save");
        return ResponseEntity.ok(service.importFile(file, user));
    }

    @PostMapping("/{id}/reimport")
    public ResponseEntity<InvoiceImportResultDTO> reimport(
            @AuthenticationPrincipal UserEntity user,
            @PathVariable("id") Long id
    ) {
        permission("invoice-save");
        return ResponseEntity.ok(service.reimport(id, user));
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
