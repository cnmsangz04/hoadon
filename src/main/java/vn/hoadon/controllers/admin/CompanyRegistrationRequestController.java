package vn.hoadon.controllers.admin;

import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.CompanyRegistrationRequestEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.CompanyRegistrationRequestRepository;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.services.CompanyService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/administrator/company-registration")
public class CompanyRegistrationRequestController extends BaseController {

    private static final Logger log = LoggerFactory.getLogger(CompanyRegistrationRequestController.class);

    @Autowired private CompanyRegistrationRequestRepository repository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private CompanyService companyService;

    @PostMapping("/list")
    public ResponseEntity<?> list(@RequestBody(required = false) FilterDTO filter,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "10") int size) {
        permission("company-list");
        try {
            Specification<CompanyRegistrationRequestEntity> spec = (root, query, cb) -> {
                List<Predicate> predicates = new ArrayList<>();
                if (filter != null) {
                    if (filter.getStatus() != null) {
                        predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                    }
                    if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                        String like = "%" + filter.getKeyword().trim() + "%";
                        predicates.add(cb.or(
                                cb.like(root.get("companyName"), like),
                                cb.like(root.get("taxcode"), like),
                                cb.like(root.get("email"), like)
                        ));
                    }
                }
                return cb.and(predicates.toArray(new Predicate[0]));
            };

            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
            return ResponseEntity.ok(repository.findAll(spec, pageable));
        } catch (Exception e) {
            log.error("Không thể tải danh sách đăng ký", e);
            return ResponseEntity.status(500).body(Map.of("message", "Không thể tải danh sách đăng ký"));
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable Long id) {
        permission("company-manage");
        try {
            CompanyRegistrationRequestEntity req = repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ đăng ký"));
            if (!Integer.valueOf(0).equals(req.getStatus())) {
                throw new IllegalArgumentException("Hồ sơ đăng ký đã được xử lý");
            }
            if (companyRepository.existsByTaxcode(req.getTaxcode())) {
                throw new IllegalArgumentException("Mã số thuế đã được sử dụng bởi công ty khác");
            }

            CompanyEntity company = new CompanyEntity();
            company.setName(req.getCompanyName());
            company.setTaxcode(req.getTaxcode());
            company.setAddress(req.getAddress());
            company.setEmail(req.getEmail());
            company.setHotline(req.getPhone());
            company.setContactName(req.getContactName());
            company.setContactMail(req.getEmail());
            company.setContactPhone(req.getPhone());
            company.setStatus(2);

            CompanyEntity saved = companyService.saveOrUpdate(company);
            UserEntity reviewer = currentUser();
            req.setStatus(1);
            req.setCompanyId(saved.getId());
            req.setReviewedAt(LocalDateTime.now());
            req.setReviewedById(reviewer != null ? reviewer.getId() : null);
            req.setReviewedByName(resolveReviewerName(reviewer));
            repository.save(req);

            return ResponseEntity.ok(Map.of(
                    "message", "Đã duyệt đăng ký, tạo công ty và gửi mail thông tin đăng nhập",
                    "companyId", saved.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể duyệt hồ sơ đăng ký", e);
            return ResponseEntity.status(500).body(Map.of("message", "Không thể duyệt hồ sơ đăng ký"));
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable Long id, @RequestBody(required = false) RejectDTO dto) {
        permission("company-manage");
        try {
            CompanyRegistrationRequestEntity req = repository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy hồ sơ đăng ký"));
            if (!Integer.valueOf(0).equals(req.getStatus())) {
                throw new IllegalArgumentException("Hồ sơ đăng ký đã được xử lý");
            }
            req.setStatus(2);
            req.setNote(dto != null ? dto.getNote() : null);
            req.setReviewedAt(LocalDateTime.now());
            UserEntity reviewer = currentUser();
            req.setReviewedById(reviewer != null ? reviewer.getId() : null);
            req.setReviewedByName(resolveReviewerName(reviewer));
            repository.save(req);
            return ResponseEntity.ok(Map.of("message", "Đã từ chối đăng ký"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("Không thể từ chối hồ sơ đăng ký", e);
            return ResponseEntity.status(500).body(Map.of("message", "Không thể từ chối hồ sơ đăng ký"));
        }
    }

    public static class FilterDTO {
        private String keyword;
        private Integer status;

        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }

    public static class RejectDTO {
        private String note;

        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }

    private String resolveReviewerName(UserEntity user) {
        if (user == null) return null;
        if (user.getName() != null && !user.getName().isBlank()) return user.getName();
        return user.getUsername();
    }
}
