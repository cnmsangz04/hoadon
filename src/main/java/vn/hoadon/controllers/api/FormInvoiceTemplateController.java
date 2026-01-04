package vn.hoadon.controllers.api;

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
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.repositories.FormInvoiceTemplateRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/form-invoices")
public class FormInvoiceTemplateController {

    @Autowired
    private FormInvoiceTemplateRepository templateRepository;

    @GetMapping("/templates")
    public ResponseEntity<?> getTemplates(
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "category", required = false) Integer category,
            @RequestParam(name = "type", required = false) Integer type,
            @RequestParam(name = "system", required = false, defaultValue = "0") Integer system,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size
    ) {
        // Normalize pageable (page index is 0-based)
        int pageIndex = Math.max(page - 1, 0);
        Pageable pageable = PageRequest.of(pageIndex, size, Sort.by(Sort.Direction.DESC, "updatedAt"));

        Page<FormInvoiceEntity> result = templateRepository.searchTemplates(q, category, type, system, pageable);

        // Return as simple structure compatible with frontend expectations
        List<TemplateItem> items = result.getContent().stream().map(TemplateItem::fromEntity).collect(Collectors.toList());
        return ResponseEntity.ok(new PageDTO<>(items, result.getNumber() + 1, result.getSize(), result.getTotalElements(), result.getTotalPages()));
    }

    public static class TemplateItem {
        public Long id;
        public String name;
        public String photo;
        public Integer type;
        public Integer category;

        public static TemplateItem fromEntity(FormInvoiceEntity e) {
            TemplateItem it = new TemplateItem();
            it.id = e.getId();
            it.name = e.getName();
            it.photo = e.getPhoto();
            it.type = e.getType();
            it.category = e.getCategory();
            return it;
        }
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