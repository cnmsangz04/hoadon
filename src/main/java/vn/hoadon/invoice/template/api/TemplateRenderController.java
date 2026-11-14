package vn.hoadon.invoice.template.api;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.invoice.template.render.TemplateRenderService;
import vn.hoadon.invoice.template.service.*;
import java.util.Map;

@RestController
@RequestMapping("/v1")
public class TemplateRenderController extends BaseController {
    private final TemplateRenderService renders;
    public TemplateRenderController(TemplateRenderService renders) { this.renders = renders; }

    private TemplateAccess access(boolean write) {
        permission(write ? "form-invoice-save|form-invoice-manage" : "form-invoice-list|form-invoice-manage");
        var user = currentUser();
        if (user == null) throw TemplateException.missing();
        return new TemplateAccess(user.getCompanyId(), user.getId(), hasPermission("form-invoice-manage"));
    }

    @GetMapping("/template-render-jobs/{id}")
    public Object job(@PathVariable Long id) { return summary(renders.job(id, access(false))); }

    @PostMapping("/template-render-jobs/{id}/retry")
    public Object retry(@PathVariable Long id) { return summary(renders.retry(id, access(true))); }

    @GetMapping("/template-artifacts/{id}/content")
    public ResponseEntity<byte[]> content(@PathVariable Long id, @RequestParam(defaultValue = "inline") String disposition) {
        var artifact = renders.artifact(id, access(false));
        return renders.response(artifact, "attachment".equals(disposition), "invoice-" + id + ".pdf");
    }

    public static Map<String, Object> summary(vn.hoadon.invoice.template.persistence.InvoiceRenderJobEntity job) {
        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("jobId", job.getId());
        response.put("state", job.getState());
        response.put("artifactId", job.getArtifactId());
        response.put("inputHash", job.getInputHash());
        response.put("errorCode", job.getErrorCode());
        response.put("message", job.getErrorMessage());
        return response;
    }
}
