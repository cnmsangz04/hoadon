package vn.hoadon.controllers.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import vn.hoadon.dto.tax.VatRatesDto;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.entity.VatRatesEntity;
import vn.hoadon.services.VatRatesService;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("v1/administrator/tax-rate")
public class VatRatesController {

    @Autowired
    private VatRatesService vatRatesService;

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return ((UserEntity) auth.getPrincipal()).getId();
    }

    // 1️⃣ Create (nhận DTO)
    @PostMapping
    public VatRatesEntity create(@RequestBody VatRatesDto dto) {
        VatRatesEntity entity = new VatRatesEntity();
        entity.setCode(dto.getCode());
        entity.setLabel(dto.getLabel());
        entity.setStatus(dto.getStatus());
        entity.setUserId(Math.toIntExact(getCurrentUserId()));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        return vatRatesService.create(entity);
    }

    // 2️⃣ Read - get all
    @GetMapping
    public List<VatRatesEntity> findAll() {
        return vatRatesService.findAll();
    }

    // 3️⃣ Update (nhận DTO)
    @PutMapping("/{id}")
    public VatRatesEntity update(
            @PathVariable Integer id,
            @RequestBody VatRatesDto dto
    ) {
        VatRatesEntity entity = new VatRatesEntity();
        entity.setLabel(dto.getLabel());
        entity.setCode(dto.getCode());
        entity.setStatus(dto.getStatus());
        entity.setUpdatedAt(LocalDateTime.now());

        return vatRatesService.update(id, entity);
    }

    // 4️⃣ Delete
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        vatRatesService.delete(id);
    }

    @PutMapping("/{id}/toggle-status")
    public VatRatesEntity toggleStatus(@PathVariable Integer id) {
        VatRatesEntity existing = vatRatesService.findById(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TaxRate not found");
        }

        // Toggle Integer status: 1 <-> 0
        Integer currentStatus = existing.getStatus();
        existing.setStatus(currentStatus != null && currentStatus == 1 ? 0 : 1);
        existing.setUpdatedAt(LocalDateTime.now());

        return vatRatesService.update(id, existing);
    }

}
