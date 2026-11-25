package vn.hoadon.invoice.template.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.invoice.template.persistence.InvoiceTemplateVersionEntity;
import vn.hoadon.invoice.template.persistence.InvoiceTemplateVersionRepository;
import vn.hoadon.repositories.FormInvoiceRepository;

import java.util.Objects;

@Service
public class InvoiceFormTemplateService {
    public record InvoiceType(Integer category, Integer stockIssueType) {}

    private final FormInvoiceRepository forms;
    private final InvoiceTemplateVersionRepository versions;
    private final InvoiceTemplateService templates;

    public InvoiceFormTemplateService(FormInvoiceRepository forms, InvoiceTemplateVersionRepository versions, InvoiceTemplateService templates) {
        this.forms = forms;
        this.versions = versions;
        this.templates = templates;
    }

    @Transactional
    public InvoiceTemplateService.Details editable(Long formId, String key, TemplateAccess access, JsonNode initialLayout) {
        var form = forms.lockById(formId).orElseThrow(TemplateException::missing);
        if (access.systemManager() ? !Objects.equals(form.getSystem(), 1)
                : !Objects.equals(form.getSystem(), 0) || !Objects.equals(form.getCompanyId(), access.companyId())) throw TemplateException.missing();
        if (form.getTemplateId() != null) {
            templates.trimHistory(form.getTemplateId(), access);
            var details = templates.ensureDraft(form.getTemplateId(), access);
            access.requireWrite(details.template());
            validateBinding(form, access);
            return details;
        }
        var details = form.getTemplateVersionId() == null
                ? templates.create(form.getName(), form.getCategory(), form.getStockIssueType(), null,
                access.systemManager(), key, access, initialLayout)
                : templates.clonePublished(form.getName(), form.getCategory(), form.getTemplateVersionId(), access.systemManager(), key, access);
        form.setTemplateId(details.template().getId());
        if (!details.versions().isEmpty()) form.setTemplateVersionId(details.versions().getFirst().getId());
        forms.saveAndFlush(form);
        validateBinding(form, access);
        templates.refreshThumbnail(details);
        return details;
    }

    @Transactional
    public InvoiceTemplateService.Details editable(Long formId, String key, TemplateAccess access) {
        return editable(formId, key, access, null);
    }

    @Transactional(readOnly = true)
    public InvoiceType invoiceType(Long formId, TemplateAccess access) {
        var form = forms.findById(formId).orElseThrow(TemplateException::missing);
        if (access.systemManager() ? !Objects.equals(form.getSystem(), 1)
                : !Objects.equals(form.getSystem(), 0) || !Objects.equals(form.getCompanyId(), access.companyId())) {
            throw TemplateException.missing();
        }
        return new InvoiceType(form.getCategory(), form.getStockIssueType());
    }

    @Transactional
    public InvoiceTemplateVersionEntity saveInitialVersion(Long formId, TemplateAccess access) {
        var form = forms.lockById(formId).orElseThrow(TemplateException::missing);
        if (access.systemManager() ? !Objects.equals(form.getSystem(), 1)
                : !Objects.equals(form.getSystem(), 0) || !Objects.equals(form.getCompanyId(), access.companyId())) {
            throw TemplateException.missing();
        }
        if (form.getTemplateId() == null) {
            editable(formId, "initial-version-" + formId, access, null);
            form = forms.lockById(formId).orElseThrow(TemplateException::missing);
        }
        validateBinding(form, access);
        if (form.getTemplateVersionId() != null) {
            return versions.findById(form.getTemplateVersionId()).orElseThrow(TemplateException::missing);
        }
        var version = templates.saveInitialVersion(form.getTemplateId(), access);
        form.setTemplateVersionId(version.getId());
        forms.saveAndFlush(form);
        return version;
    }

    @Transactional(readOnly = true)
    public InvoiceTemplateVersionEntity latestPublished(Long formId) {
        var form = forms.findById(formId).orElseThrow(TemplateException::missing);
        if (form.getTemplateVersionId() == null)
            throw TemplateException.invalid("TEMPLATE_NOT_PUBLISHED", "Mẫu chưa có phiên bản để sử dụng. Mở Thiết kế và chọn Lưu phiên bản.");
        var assigned = versions.findById(form.getTemplateVersionId()).orElseThrow(TemplateException::missing);
        if (form.getTemplateId() == null || !Objects.equals(assigned.getTemplateId(), form.getTemplateId())) {
            throw TemplateException.invalid("TEMPLATE_BINDING_INVALID", "Mẫu hóa đơn chưa liên kết đúng với phiên bản thiết kế");
        }
        var template = templates.require(form.getTemplateId(), new TemplateAccess(form.getCompanyId(), null, false), false);
        validateOwnership(form, template, null);
        if (!"ACTIVE".equals(template.getState()))
            throw TemplateException.invalid("TEMPLATE_ARCHIVED", "Mẫu thiết kế đã ngừng sử dụng. Vui lòng chọn mẫu khác.");
        return versions.findTopByTemplateIdOrderByVersionNoDesc(form.getTemplateId()).orElseThrow(TemplateException::missing);
    }

    @Transactional(readOnly = true)
    public InvoiceTemplateVersionEntity published(Long formId) {
        return latestPublished(formId);
    }

    @Transactional
    public void deleteForForm(Long formId, TemplateAccess access) {
        var form = forms.lockById(formId).orElseThrow(TemplateException::missing);
        if (access.systemManager() ? !Objects.equals(form.getSystem(), 1)
                : !Objects.equals(form.getSystem(), 0) || !Objects.equals(form.getCompanyId(), access.companyId())) {
            throw TemplateException.missing();
        }
        if (form.getTemplateId() != null) {
            templates.deleteOwned(form.getTemplateId(), formId, access);
        }
    }

    @Transactional(readOnly = true)
    public void validateBinding(vn.hoadon.entity.FormInvoiceEntity form, TemplateAccess access) {
        if (form.getTemplateId() == null) {
            if (form.getTemplateVersionId() != null) {
                throw TemplateException.invalid("TEMPLATE_BINDING_INVALID", "Mẫu hóa đơn có phiên bản nhưng chưa có template sở hữu");
            }
            return;
        }
        var template = templates.require(form.getTemplateId(), access, false);
        validateOwnership(form, template, access);
        if (!Objects.equals(template.getCategory(), form.getCategory())) {
            throw TemplateException.invalid("TEMPLATE_INCOMPATIBLE", "Loại hóa đơn của form và template không khớp");
        }
        if (form.getTemplateVersionId() != null) {
            var version = versions.findById(form.getTemplateVersionId()).orElseThrow(TemplateException::missing);
            if (!Objects.equals(version.getTemplateId(), template.getId())) {
                throw TemplateException.invalid("TEMPLATE_BINDING_INVALID", "Phiên bản không thuộc template của mẫu hóa đơn");
            }
        }
    }

    private void validateOwnership(vn.hoadon.entity.FormInvoiceEntity form,
                                   vn.hoadon.invoice.template.persistence.InvoiceTemplateEntity template,
                                   TemplateAccess access) {
        boolean systemForm = Objects.equals(form.getSystem(), 1);
        if (systemForm) {
            if (!"SYSTEM".equals(template.getScope())) throw TemplateException.invalid("TEMPLATE_BINDING_INVALID", "Mẫu hệ thống phải dùng template hệ thống riêng");
            if (access != null && !access.systemManager()) throw TemplateException.missing();
            return;
        }
        if (!Objects.equals(form.getCompanyId(), template.getCompanyId()) || !"COMPANY".equals(template.getScope())) {
            throw TemplateException.invalid("TEMPLATE_BINDING_INVALID", "Mẫu của công ty phải sở hữu template riêng");
        }
    }
}
