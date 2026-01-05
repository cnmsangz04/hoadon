package vn.hoadon.controllers.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.dto.company.CompanyFilterDTO;
import vn.hoadon.services.CompanyService;

import java.util.Optional;

@RestController
@RequestMapping("/v1/administrator/company")
public class CompanyController {

    private static final Logger log =
            LoggerFactory.getLogger(CompanyController.class);

    @Autowired
    private CompanyService service;

    @PostMapping("/list")
    public ResponseEntity<?> list(
            @RequestBody CompanyFilterDTO filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        try {
            Pageable pageable =
                    PageRequest.of(page, size, Sort.by("createdAt").descending());

            Page<CompanyEntity> pageData =
                    service.list(filter, pageable);

            return ResponseEntity.ok(pageData);
        } catch (Exception e) {
            log.error("Error listing companies", e);
            return ResponseEntity.status(500)
                    .body(java.util.Map.of("message", "Không thể tải danh sách công ty"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        try {
            Optional<CompanyEntity> company = service.findById(id);

            log.info("found = {}", company.isPresent());

            if (company.isPresent()) {
                return ResponseEntity.ok(company.get());
            } else {
                return ResponseEntity.status(404)
                        .body(java.util.Map.of("message", "Không tìm thấy công ty"));
            }
        } catch (Exception e) {
            log.error("Error getting company", e);
            return ResponseEntity.status(500)
                    .body(java.util.Map.of("message", "Không thể tải thông tin công ty"));
        }
    }

    @PostMapping("/saveOrUpdate")
    public ResponseEntity<?> saveOrUpdate(
            @RequestBody CompanyEntity company) {
        try {
            CompanyEntity saved = service.saveOrUpdate(company);

            log.info("saved id = {}", saved.getId());

            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            log.warn("Validation error saving company", e);
            return ResponseEntity.status(400)
                    .body(java.util.Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error saving company", e);
            return ResponseEntity.status(500)
                    .body(java.util.Map.of("message", "Không thể lưu thông tin công ty"));
        }
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long id, @RequestBody StatusDTO req) {
        try {
            service.updateStatus(id, req.getStatus());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("Validation error updating status", e);
            return ResponseEntity.status(400)
                    .body(java.util.Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating company status", e);
            return ResponseEntity.status(500)
                    .body(java.util.Map.of("message", "Không thể cập nhật trạng thái công ty"));
        }
    }

    @PostMapping("/{id}/send-credentials")
    public ResponseEntity<?> sendAdminCredentials(@PathVariable("id") Long companyId) {
        try {
            service.sendAdminCredentials(companyId);
            return ResponseEntity.ok(java.util.Map.of("message", "Đã gửi thông tin tài khoản quản trị tới email"));
        } catch (IllegalArgumentException e) {
            log.warn("Validation error sending admin credentials", e);
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Error sending admin credentials", e);
            return ResponseEntity.status(500)
                    .body(java.util.Map.of("message", "Không thể gửi thông tin tài khoản quản trị"));
        }
    }
}

class StatusDTO {
    private Integer status;
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}