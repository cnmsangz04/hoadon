package vn.hoadon.invoice.template.api;

import org.springframework.web.bind.annotation.*;

import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.invoice.template.service.*;

@RestController
@RequestMapping("/v1/administrator/invoice-template-fields")
public class InvoiceTemplateFieldController extends BaseController {
    private final InvoiceTemplateFieldService service;

    public InvoiceTemplateFieldController(InvoiceTemplateFieldService service) {
        this.service = service;
    }

    @GetMapping
    public Object list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String group,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(q, group, active, page, size, access());
    }

    @PostMapping
    public Object create(@RequestBody InvoiceTemplateFieldService.FieldInput body) {
        return service.create(body, access());
    }

    @PutMapping("/{id}")
    public Object update(
            @PathVariable Long id, @RequestBody InvoiceTemplateFieldService.FieldInput body) {
        return service.update(id, body, access());
    }

    @GetMapping("/metadata")
    public Object metadata() {
        return service.metadata(access());
    }

    @PostMapping("/groups")
    public Object createGroup(@RequestBody InvoiceTemplateFieldService.GroupInput body) {
        return service.saveGroup(null, body, access());
    }

    @PutMapping("/groups/{id}")
    public Object updateGroup(
            @PathVariable Long id, @RequestBody InvoiceTemplateFieldService.GroupInput body) {
        return service.saveGroup(id, body, access());
    }

    @PostMapping("/datasets")
    public Object createDataset(@RequestBody InvoiceTemplateFieldService.DatasetInput body) {
        return service.saveDataset(null, body, access());
    }

    @PutMapping("/datasets/{id}")
    public Object updateDataset(
            @PathVariable Long id, @RequestBody InvoiceTemplateFieldService.DatasetInput body) {
        return service.saveDataset(id, body, access());
    }

    private TemplateAccess access() {
        permission("form-invoice-manage");
        var user = currentUser();
        if (user == null || user.getId() == null) throw TemplateException.missing();
        return new TemplateAccess(user.getCompanyId(), user.getId(), true);
    }
}
