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
    public ResponseEntity<Page<CompanyEntity>> list(
            @RequestBody CompanyFilterDTO filter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable =
                PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<CompanyEntity> pageData =
                service.list(filter, pageable);

        return ResponseEntity.ok(pageData);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompanyEntity> get(@PathVariable Long id) {

        Optional<CompanyEntity> company = service.findById(id);

        log.info("found = {}", company.isPresent());

        return ResponseEntity.of(company);
    }

    @PostMapping("/saveOrUpdate")
    public ResponseEntity<CompanyEntity> saveOrUpdate(
            @RequestBody CompanyEntity company) {
        CompanyEntity saved = service.saveOrUpdate(company);

        log.info("saved id = {}", saved.getId());

        return ResponseEntity.ok(saved);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long id, @RequestBody StatusDTO req) {
        service.updateStatus(id, req.getStatus());
        return ResponseEntity.ok().build();
    }

}

class StatusDTO {
    private Integer status;
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}