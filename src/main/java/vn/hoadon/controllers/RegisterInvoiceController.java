package vn.hoadon.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.registerinvoice.RegisterInvoicePrefillDto;
import vn.hoadon.entity.RegisterInvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.services.RegisterInvoiceService;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/v1/register-invoices")
public class RegisterInvoiceController extends BaseController {
    private final RegisterInvoiceService service;

    public RegisterInvoiceController(RegisterInvoiceService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegisterInvoiceEntity> getById(@PathVariable Long id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<RegisterInvoiceEntity>> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long userId) {
        if (companyId != null) {
            return ResponseEntity.ok(service.findByCompany(companyId));
        }
        if (userId != null) {
            return ResponseEntity.ok(service.findByUser(userId));
        }
        // Fallback: use current user context
        UserEntity user = currentUser();
        if (user != null) {
            // prefer company scope if available
            if (user.getCompanyId() != null) {
                return ResponseEntity.ok(service.findByCompany(user.getCompanyId()));
            }
            return ResponseEntity.ok(service.findByUser(user.getId()));
        }
        // default: return empty to avoid exposing all
        return ResponseEntity.ok(List.of());
    }

    // Prefill data for create.vue - auto-detect companyId from authenticated user
    @GetMapping("/prefill")
    public ResponseEntity<RegisterInvoicePrefillDto> prefill(
            @RequestParam(required = false) Integer declarationType,
            @RequestParam(required = false) LocalDate effectiveDate) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<RegisterInvoicePrefillDto> dto = service.prefill(user.getCompanyId(), declarationType, effectiveDate);
        return dto.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<RegisterInvoiceEntity> create(@RequestBody RegisterInvoiceEntity body) {
        RegisterInvoiceEntity created = service.create(body);
        return ResponseEntity.created(URI.create("/api/register-invoices/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RegisterInvoiceEntity> update(@PathVariable Long id, @RequestBody RegisterInvoiceEntity body) {
        Optional<RegisterInvoiceEntity> updated = service.update(id, body);
        return updated.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Void> sendToCQT(@PathVariable Long id) {
        // TODO: implement integration with CQT; for now 202 Accepted
        return ResponseEntity.accepted().build();
    }
}