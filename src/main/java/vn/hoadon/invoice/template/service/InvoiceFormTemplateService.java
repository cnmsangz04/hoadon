package vn.hoadon.invoice.template.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.invoice.template.persistence.InvoiceTemplateVersionEntity;
import vn.hoadon.invoice.template.persistence.InvoiceTemplateVersionRepository;
import vn.hoadon.repositories.FormInvoiceRepository;

import java.util.Objects;

@Service
public class InvoiceFormTemplateService {
    private final FormInvoiceRepository forms;
    private final InvoiceTemplateVersionRepository versions;
    private final InvoiceTemplateService templates;

    public InvoiceFormTemplateService(FormInvoiceRepository forms, InvoiceTemplateVersionRepository versions, InvoiceTemplateService templates) {
        this.forms = forms;
        this.versions = versions;
        this.templates = templates;
    }

    @Transactional
    public InvoiceTemplateService.Details editable(Long formId, String key, TemplateAccess access) {
        var form = forms.lockById(formId).orElseThrow(TemplateException::missing);
        if (access.systemManager() ? !Objects.equals(form.getSystem(), 0)
                : !Objects.equals(form.getSystem(), 1) || !Objects.equals(form.getCompanyId(), access.companyId())) throw TemplateException.missing();
        if (form.getTemplateId() != null) {
            var details = templates.details(form.getTemplateId(), access);
            access.requireWrite(details.template());
            return details;
        }
        var details = templates.create(form.getName(), form.getCategory(), form.getTemplateVersionId(), access.systemManager(), key, access);
        form.setTemplateId(details.template().getId());
        forms.saveAndFlush(form);
        return details;
    }

    @Transactional(readOnly = true)
    public InvoiceTemplateVersionEntity published(Long formId) {
        var form = forms.findById(formId).orElseThrow(TemplateException::missing);
        if (form.getTemplateVersionId() == null)
            throw TemplateException.invalid("TEMPLATE_NOT_PUBLISHED", "Mẫu chưa được công bố. Mở Thiết kế mẫu, kiểm tra và công bố rồi chọn Dùng cho hóa đơn mới.");
        var version = versions.findById(form.getTemplateVersionId()).orElseThrow(TemplateException::missing);
        var template = templates.require(version.getTemplateId(), new TemplateAccess(form.getCompanyId(), null, false), false);
        if (!"ACTIVE".equals(template.getState())) throw TemplateException.invalid("TEMPLATE_ARCHIVED", "Mẫu thiết kế đã ngừng sử dụng. Vui lòng chọn phiên bản khác cho hóa đơn mới.");
        return version;
    }
}
