package vn.hoadon.controllers.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import vn.hoadon.dto.taxauthority.TaxAuthorityResponse;
import vn.hoadon.dto.taxauthority.TaxAuthorityRequest;
import vn.hoadon.services.TaxAuthorityService;

@RestController
@RequestMapping("/v1/tax-authorities")
@CrossOrigin(origins = "*", allowedHeaders = "*") // Bật nếu test local khác port
public class TaxAuthorityController {

    @Autowired
    private TaxAuthorityService taxService;

    // GET /v1/tax-authorities?page=0&size=10&keyword=...
    @GetMapping
    public ResponseEntity<?> getList(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long parentId, // Thêm dòng này (có thể null)
            @RequestParam(required = false) Integer status, // Thêm dòng này (có thể null)
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(defaultValue = "id") String sortField
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc") 
                ? Sort.by(sortField).ascending() 
                : Sort.by(sortField).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        
        // Gọi Service với đầy đủ tham số lọc
        Page<TaxAuthorityResponse> result = taxService.search(keyword, parentId, status, pageable);
        
        return ResponseEntity.ok(result);
    }

    // GET /v1/tax-authorities/{id}
    @GetMapping("/{id}")
    public ResponseEntity<?> getDetail(@PathVariable Long id) {
        return ResponseEntity.ok(taxService.findById(id));
    }

    // POST /v1/tax-authorities
    @PostMapping
    public ResponseEntity<?> create(@RequestBody TaxAuthorityRequest request) {
        // Có thể thêm @Valid vào trước @RequestBody nếu dùng Validation
        return ResponseEntity.ok(taxService.create(request));
    }

    // PUT /v1/tax-authorities/{id}
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody TaxAuthorityRequest request) {
        return ResponseEntity.ok(taxService.update(id, request));
    }

    // DELETE /v1/tax-authorities/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        taxService.delete(id);
        return ResponseEntity.ok("Đã xóa thành công");
    }
}