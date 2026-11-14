package vn.hoadon.invoice.template.api;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.invoice.template.catalog.InvoiceFieldCatalog;
import vn.hoadon.invoice.template.service.*;
import vn.hoadon.invoice.template.storage.InvoiceAssetService;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping({"/v1/invoice-templates", "/v1/administrator/invoice-templates"})
public class InvoiceTemplateController extends BaseController {
    public record CreateRequest(@NotBlank @Size(max = 255) String name, @Min(1) @Max(2) int category, Long sourceVersionId) { }
    public record SaveRequest(@NotNull Long expectedLockVersion, @NotNull JsonNode layout) { }
    public record RevisionRequest(@NotNull Long expectedLockVersion) { }
    public record DesignerPreviewRequest(@NotNull JsonNode layout, @Min(0) @Max(60) int rows) { }
    public record PublishRequest(@NotNull Long expectedLockVersion, @NotNull Long validationReportId) { }
    public record RestoreRequest(@NotNull Long expectedLockVersion, @NotNull Long versionId) { }

    private final InvoiceTemplateService service;
    private final InvoiceFieldCatalog catalog;
    private final InvoiceAssetService assets;
    private final InvoiceFormTemplateService formTemplates;
    private final InvoiceTemplateBindingService bindings;

    public InvoiceTemplateController(InvoiceTemplateService service, InvoiceFieldCatalog catalog, InvoiceAssetService assets, InvoiceFormTemplateService formTemplates, InvoiceTemplateBindingService bindings) {
        this.service = service;
        this.catalog = catalog;
        this.assets = assets;
        this.formTemplates = formTemplates;
        this.bindings = bindings;
    }

    private TemplateAccess access(HttpServletRequest request, boolean write) {
        boolean system = request.getRequestURI().contains("/administrator/");
        permission(system ? "form-invoice-manage" : write ? "form-invoice-save" : "form-invoice-list");
        var user = currentUser();
        if (user == null || user.getId() == null || (!system && user.getCompanyId() == null)) throw TemplateException.missing();
        return new TemplateAccess(user.getCompanyId(), user.getId(), system);
    }

    @GetMapping
    public Object list(HttpServletRequest request, @RequestParam(defaultValue = "COMPANY") String scope,
                       @RequestParam(required = false) String q, @RequestParam(required = false) Integer category,
                       @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size) {
        var access = access(request, false);
        return service.list(access, request.getRequestURI().contains("/administrator/") ? "SYSTEM" : scope, q, category, page, size);
    }

    @GetMapping("/catalog")
    public Object catalog(HttpServletRequest request, @RequestParam(required = false) String version) {
        access(request, false);
        var source = version == null ? catalog.latest() : catalog.snapshot(version);
        var fields = source.fields().stream().map(field -> {
            Map<String, Object> row = new java.util.LinkedHashMap<>();
            row.put("key", field.key()); row.put("label", field.label()); row.put("group", field.group());
            row.put("dataset", field.dataset()); row.put("repeated", field.repeated());
            row.put("rowScope", source.rowScope(field.dataset()));
            row.put("valueType", field.valueType()); row.put("defaultFormatter", field.defaultFormatter()); row.put("active", field.active());
            return row;
        }).toList();
        return Map.of("version", source.code(), "fields", fields, "groups", source.groups(), "datasets", source.datasets());
    }

    @GetMapping("/default-layout")
    public Object defaultLayout(HttpServletRequest request) {
        access(request, false);
        return service.tree(service.defaultLayout());
    }

    @GetMapping("/designer-fonts")
    public Object designerFonts(HttpServletRequest request) {
        access(request, true);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("css", service.designerFontCss()));
    }

    @PostMapping("/{id}/designer-preview")
    public Object designerPreview(@PathVariable Long id, @Valid @RequestBody DesignerPreviewRequest body, HttpServletRequest request) {
        String html = service.designerPreview(id, body.layout(), body.rows(), access(request, true));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("html", html));
    }

    @PostMapping
    public Object create(HttpServletRequest request, @Valid @RequestBody CreateRequest body, @RequestHeader("Idempotency-Key") String key) {
        var access = access(request, true);
        return service.create(body.name(), body.category(), body.sourceVersionId(), access.systemManager(), key, access);
    }

    @GetMapping("/{id}")
    public Object details(@PathVariable Long id, HttpServletRequest request) { return service.details(id, access(request, false)); }

    @PostMapping("/from-form/{id}")
    public Object fromForm(@PathVariable Long id, @RequestHeader("Idempotency-Key") String key, HttpServletRequest request) {
        return formTemplates.editable(id, key, access(request, true));
    }

    @PostMapping("/versions/{versionId}/bind/{formId}")
    public ResponseEntity<Void> bind(@PathVariable Long versionId, @PathVariable Long formId, HttpServletRequest request) {
        bindings.bindForm(formId, versionId, access(request, true));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/draft")
    public Object save(@PathVariable Long id, @Valid @RequestBody SaveRequest body, HttpServletRequest request) {
        return service.save(id, body.expectedLockVersion(), body.layout(), access(request, true));
    }

    @PostMapping("/{id}/preview")
    public ResponseEntity<?> preview(@PathVariable Long id, @Valid @RequestBody RevisionRequest body,
                                     @RequestParam(defaultValue = "3") int rows, HttpServletRequest request) {
        return ResponseEntity.accepted().body(TemplateRenderController.summary(service.preview(id, body.expectedLockVersion(), rows, access(request, true))));
    }

    @PostMapping("/{id}/validate")
    public ResponseEntity<?> validate(@PathVariable Long id, @Valid @RequestBody RevisionRequest body, HttpServletRequest request) {
        return ResponseEntity.accepted().body(service.validate(id, body.expectedLockVersion(), access(request, true)));
    }

    @GetMapping("/validation-runs/{id}")
    public Object validation(@PathVariable Long id, HttpServletRequest request) { return service.validation(id, access(request, true)); }

    @PostMapping("/{id}/publish")
    public Object publish(@PathVariable Long id, @Valid @RequestBody PublishRequest body, @RequestHeader("Idempotency-Key") String key, HttpServletRequest request) {
        return service.publish(id, body.expectedLockVersion(), body.validationReportId(), key, access(request, true));
    }

    @PostMapping("/{id}/draft-from-version")
    public Object restore(@PathVariable Long id, @Valid @RequestBody RestoreRequest body, HttpServletRequest request) {
        return service.restore(id, body.versionId(), body.expectedLockVersion(), access(request, true));
    }

    @PostMapping("/{id}/upgrade-catalog")
    public Object upgradeCatalog(@PathVariable Long id, @Valid @RequestBody RevisionRequest body, HttpServletRequest request) {
        return service.upgradeCatalog(id, body.expectedLockVersion(), access(request, true));
    }

    @PostMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable Long id, @Valid @RequestBody RevisionRequest body, HttpServletRequest request) {
        service.archive(id, body.expectedLockVersion(), access(request, true));
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/assets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object upload(@RequestParam MultipartFile file, @RequestParam String kind, HttpServletRequest request) throws IOException {
        var access = access(request, true);
        return assets.upload(file.getBytes(), kind, access, access.systemManager());
    }
}
