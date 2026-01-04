package vn.hoadon.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.dto.InvoiceDTO;
import vn.hoadon.services.InvoiceService;

import java.util.List;

@RestController
@RequestMapping("/v1/invoices")
public class InvoiceController {

    @Autowired
    private InvoiceService invoiceService;

    @GetMapping
    public ResponseEntity<?> list(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "status", required = false) Short status,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        int pageIndex = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<InvoiceDTO> result = invoiceService.search(q, status, pageable);
        List<InvoiceDTO> items = result.getContent();
        PageDTO<InvoiceDTO> dto = new PageDTO<>(items, result.getNumber() + 1, result.getSize(), result.getTotalElements(), result.getTotalPages());
        return ResponseEntity.ok(dto);
    }

    public static class PageDTO<T> {
        public List<T> items;
        public int current_page;
        public int per_page;
        public long total;
        public int last_page;

        public PageDTO(List<T> items, int currentPage, int perPage, long total, int lastPage) {
            this.items = items;
            this.current_page = currentPage;
            this.per_page = perPage;
            this.total = total;
            this.last_page = lastPage;
        }
    }
}
