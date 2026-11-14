package vn.hoadon.invoice.template.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.invoice.template.service.*;

@RestController
@RequestMapping("/v1/template-import-jobs")
public class TemplateImportController extends BaseController {
    public record CreateRequest(@NotNull Long sourcePdfAssetId, Long targetTemplateId, Long expectedDraftLockVersion) { }
    public record ApplyRequest(@NotBlank @Size(max = 255) String name, @Min(1) @Max(2) int category) { }
    private final TemplateImportService service;
    public TemplateImportController(TemplateImportService service) { this.service = service; }

    private TemplateAccess access() {
        permission("form-invoice-save");
        var user = currentUser();
        if (user == null) throw TemplateException.missing();
        return new TemplateAccess(user.getCompanyId(), user.getId(), false);
    }

    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody CreateRequest request) {
        return ResponseEntity.accepted().body(service.create(request.sourcePdfAssetId(), request.targetTemplateId(), request.expectedDraftLockVersion(), access()));
    }

    @GetMapping("/{id}")
    public Object get(@PathVariable Long id) { return service.get(id, access()); }

    @PostMapping("/{id}/apply")
    public Object apply(@PathVariable Long id, @Valid @RequestBody ApplyRequest request) { return service.apply(id, request.name(), request.category(), access()); }
}
