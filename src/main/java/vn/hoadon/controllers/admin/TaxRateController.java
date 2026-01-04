package vn.hoadon.controllers.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.hoadon.dto.tax.TaxRateDto;
import vn.hoadon.entity.TaxRateEntity;
import vn.hoadon.services.TaxRateService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("v1/administrator/tax-rate")
public class TaxRateController {

    @Autowired
    private TaxRateService taxRateService;

    // 1️⃣ Create (nhận DTO)
    @PostMapping
    public TaxRateEntity create(@RequestBody TaxRateDto dto) {
        TaxRateEntity entity = new TaxRateEntity();
        entity.setRate(dto.getRate());
        entity.setStatus(dto.getStatus());
        entity.setCreatedAt(LocalDateTime.now());

        return taxRateService.create(entity);
    }

    // 2️⃣ Read - get all
    @GetMapping
    public List<TaxRateEntity> findAll() {
        return taxRateService.findAll();
    }

    // 3️⃣ Update (nhận DTO)
    @PutMapping("/{id}")
    public TaxRateEntity update(
            @PathVariable Integer id,
            @RequestBody TaxRateDto dto
    ) {
        TaxRateEntity entity = new TaxRateEntity();
        entity.setRate(dto.getRate());
        entity.setStatus(dto.getStatus());

        return taxRateService.update(id, entity);
    }

    // 4️⃣ Delete
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        taxRateService.delete(id);
    }

    @PutMapping("/{id}/toggle-status")
    public TaxRateEntity toggleStatus(@PathVariable Integer id) {
        TaxRateEntity existing = taxRateService.findById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TaxRate not found");
        }

        // Đảo ngược Boolean
        existing.setStatus(!existing.getStatus());

        return taxRateService.update(id, existing);
    }

}
