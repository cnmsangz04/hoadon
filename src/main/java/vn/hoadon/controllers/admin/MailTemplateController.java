package vn.hoadon.controllers.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.dto.mail.MailTemplateDto;
import vn.hoadon.services.MailTemplateService;

import java.util.List;

@RestController
@RequestMapping("v1/administrator/mail-template")
@RequiredArgsConstructor
public class MailTemplateController {

    private final MailTemplateService mailTemplateService;

    /**
     * Lấy danh sách mail template theo company
     * GET /v1/administrator/mail-template?companyId=1
     */
    @GetMapping
    public ResponseEntity<List<MailTemplateDto>> getByCompany(
            @RequestParam(defaultValue = "1") Integer companyId
    ) {
        return ResponseEntity.ok(
                mailTemplateService.getByCompanyId(companyId)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<MailTemplateDto> getById(
            @PathVariable Integer id
    ) {
        return ResponseEntity.ok(
                mailTemplateService.getById(id)
        );
    }

    /**
     * Thêm mới mail template
     * POST /v1/administrator/mail-template
     */
    @PostMapping
    public ResponseEntity<MailTemplateDto> create(
            @RequestBody @Valid MailTemplateDto dto
    ) {
        return ResponseEntity.ok(
                mailTemplateService.create(dto)
        );
    }

    /**
     * Cập nhật mail template
     * PUT /v1/administrator/mail-template/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<MailTemplateDto> update(
            @PathVariable Integer id,
            @RequestBody @Valid MailTemplateDto dto
    ) {
        return ResponseEntity.ok(
                mailTemplateService.update(id, dto)
        );
    }

    /**
     * Xóa mail template
     * DELETE /v1/administrator/mail-template/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Integer id
    ) {
        mailTemplateService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
