package vn.hoadon.controllers.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.hoadon.dto.tax.TaxRateDto;
import vn.hoadon.entity.TaxRate;
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
    public TaxRate create(@RequestBody TaxRateDto dto) {
        TaxRate entity = new TaxRate();
        entity.setRate(dto.getRate());
        entity.setStatus(dto.getStatus());
        entity.setCreatedAt(LocalDateTime.now());

        return taxRateService.create(entity);
    }

    // 2️⃣ Read - get all
    @GetMapping
    public List<TaxRate> findAll() {
        return taxRateService.findAll();
    }

    // 3️⃣ Update (nhận DTO)
    @PutMapping("/{id}")
    public TaxRate update(
            @PathVariable Integer id,
            @RequestBody TaxRateDto dto
    ) {
        TaxRate entity = new TaxRate();
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
    public TaxRate toggleStatus(@PathVariable Integer id) {
        TaxRate existing = taxRateService.findById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TaxRate not found");
        }

        // Đảo ngược Boolean
        existing.setStatus(!existing.getStatus());

        return taxRateService.update(id, existing);
    }

}
