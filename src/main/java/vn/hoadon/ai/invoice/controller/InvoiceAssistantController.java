package vn.hoadon.ai.invoice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.ai.invoice.service.InvoiceAssistantService;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.UserEntity;

@RestController
@RequestMapping("/v1/ai/invoice-assistant")
public class InvoiceAssistantController extends BaseController {
    private final InvoiceAssistantService assistantService;

    public InvoiceAssistantController(InvoiceAssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/preview")
    public ResponseEntity<InvoiceAssistantDtos.PreviewResponse> preview(
            @RequestBody InvoiceAssistantDtos.PreviewRequest request) {
        UserEntity user = requireInvoiceUser();
        return ResponseEntity.ok(assistantService.preview(user.getId(), user.getCompanyId(), request));
    }

    @PostMapping("/proposals/{id}/confirm")
    public ResponseEntity<InvoiceAssistantDtos.ConfirmResponse> confirm(@PathVariable long id) {
        UserEntity user = requireInvoiceUser();
        return ResponseEntity.ok(assistantService.confirm(id, user.getId(), user.getCompanyId()));
    }

    @PostMapping("/proposals/{id}/cancel")
    public ResponseEntity<Void> cancel(@PathVariable long id) {
        UserEntity user = requireInvoiceUser();
        assistantService.cancel(id, user.getId(), user.getCompanyId());
        return ResponseEntity.noContent().build();
    }

    private UserEntity requireInvoiceUser() {
        permission("invoice-save");
        UserEntity user = currentUser();
        if (user == null || user.getId() == null || user.getCompanyId() == null) {
            throw new AccessDeniedException("Không xác định được tài khoản hoặc doanh nghiệp");
        }
        return user;
    }
}
