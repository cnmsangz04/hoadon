package vn.hoadon.invoice.template.service;

import vn.hoadon.repositories.CompanyRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.entity.*;
import vn.hoadon.invoice.template.persistence.*;
import vn.hoadon.invoice.template.storage.InvoiceAssetService;
import vn.hoadon.repositories.FormInvoiceRepository;

import java.util.*;

@Service
public class InvoiceTemplateBindingService {
    private final FormInvoiceRepository forms;
    private final InvoiceTemplateVersionRepository versions;
    private final InvoiceTemplateRepository templates;
    private final InvoiceFormTemplateService formTemplates;
    private final InvoiceAssetService assets;
    private final CompanyInvoiceDefaultRepository defaults;
    private final CompanyRepository companies;
    private final InvoiceTemplateService templateService;

    public InvoiceTemplateBindingService(FormInvoiceRepository forms, InvoiceTemplateVersionRepository versions, InvoiceTemplateRepository templates,
                                          InvoiceFormTemplateService formTemplates, InvoiceAssetService assets,
                                          CompanyInvoiceDefaultRepository defaults, CompanyRepository companies,
                                          InvoiceTemplateService templateService) {
        this.forms = forms;
        this.versions = versions;
        this.templates = templates;
        this.formTemplates = formTemplates;
        this.assets = assets;
        this.defaults = defaults;
        this.companies = companies;
        this.templateService = templateService;
    }

    @Transactional
    public InvoiceTemplateVersionEntity publishForForm(Long formId, Long templateId, long expectedVersion,
                                                       Long reportId, String key, TemplateAccess access) {
        var form = forms.lockById(formId).orElseThrow(TemplateException::missing);
        if (access.systemManager() ? !Objects.equals(form.getSystem(), 1)
                : !Objects.equals(form.getSystem(), 0) || !Objects.equals(form.getCompanyId(), access.companyId())) {
            throw TemplateException.missing();
        }
        var version = templateService.publish(templateId, expectedVersion, reportId, key, access);
        bindForm(formId, version.getId(), access);
        return version;
    }

    @Transactional
    public FormInvoiceEntity activeForm(Long companyId, int category) {
        companies.lockById(companyId).orElseThrow(TemplateException::missing);
        var selection = defaults.findByCompanyIdAndCategory(companyId, category).orElse(null);
        if (selection == null) {
            var candidates = forms.findByCompanyIdAndSystemAndCategoryAndStatus(companyId, 0, category, 1);
            if (candidates.size() > 1) throw TemplateException.invalid("DEFAULT_FORM_CONFLICT", "Có nhiều mẫu cùng kích hoạt; cần đối chiếu trước khi chọn mặc định");
            selection = new CompanyInvoiceDefaultEntity();
            selection.setCompanyId(companyId);
            selection.setCategory(category);
            selection.setActivationPolicy("COMPAT_SINGLE_ACTIVE_PER_CATEGORY");
            selection.setFormInvoiceId(candidates.isEmpty() ? null : candidates.getFirst().getId());
            selection.setUpdatedAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC));
            defaults.save(selection);
        }
        if (selection.getFormInvoiceId() == null) return null;
        var form = forms.findById(selection.getFormInvoiceId()).orElseThrow(TemplateException::missing);
        if (!Objects.equals(form.getCompanyId(), companyId) || !Objects.equals(form.getCategory(), category) || !Objects.equals(form.getSystem(), 0) || !Objects.equals(form.getStatus(), 1)) throw TemplateException.invalid("DEFAULT_FORM_INVALID", "Mẫu mặc định không còn phù hợp; vui lòng chọn lại");
        return form;
    }

    @Transactional
    public void bindForm(Long formId, Long versionId, TemplateAccess access) {
        var form = forms.lockById(formId).orElseThrow(TemplateException::missing);
        if (access.systemManager()) {
            if (!Objects.equals(form.getSystem(), 1)) throw TemplateException.missing();
        } else if (!Objects.equals(form.getCompanyId(), access.companyId()) || !Objects.equals(form.getSystem(), 0)) throw TemplateException.missing();
        var version = versions.findById(versionId).orElseThrow(TemplateException::missing);
        var template = templates.findById(version.getTemplateId()).orElseThrow(TemplateException::missing);
        access.requireRead(template);
        if (access.systemManager() && (!Objects.equals(form.getSystem(), 1) || !"SYSTEM".equals(template.getScope()))) throw TemplateException.missing();
        if (!access.systemManager() && (!Objects.equals(form.getSystem(), 0)
                || !"COMPANY".equals(template.getScope())
                || !Objects.equals(template.getCompanyId(), access.companyId()))) {
            throw TemplateException.invalid("TEMPLATE_COPY_REQUIRED", "Mẫu hệ thống phải được sao chép thành mẫu riêng của công ty trước khi sử dụng");
        }
        if (!Objects.equals(template.getCategory(), form.getCategory())) {
            throw TemplateException.invalid("TEMPLATE_INCOMPATIBLE", "Mẫu không phù hợp loại hóa đơn");
        }
        if (form.getTemplateId() != null && !Objects.equals(form.getTemplateId(), template.getId())) {
            throw TemplateException.invalid("TEMPLATE_BINDING_INVALID", "Mỗi mẫu hóa đơn chỉ được liên kết với template riêng của mẫu đó");
        }
        boolean currentTemplate = Objects.equals(form.getTemplateId(), template.getId());
        if (!currentTemplate && !"ACTIVE".equals(template.getState())) {
            throw TemplateException.invalid("TEMPLATE_INCOMPATIBLE", "Mẫu đã ngừng sử dụng");
        }
        if (currentTemplate && !"ACTIVE".equals(template.getState())) {
            template.setState("ACTIVE");
            template.setUpdatedAt(java.time.LocalDateTime.now());
        }
        requireTaxMode(version, form);
        form.setTemplateVersionId(versionId);
        form.setTemplateId(template.getId());
    }

    @Transactional
    public void pin(InvoiceEntity invoice) {
        var form = forms.findById(invoice.getFormId().longValue()).orElseThrow(TemplateException::missing);
        Long companyId = invoice.getCompanyId().longValue();
        if (!Objects.equals(form.getSystem(), 0) || !Objects.equals(form.getCompanyId(), companyId)) throw TemplateException.missing();
        requireTaxMode(formTemplates.latestPublished(form.getId()), form);
        if (invoice.getIssueProfileSnapshotJson() == null) {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("formCode", form.getFormCode());
            profile.put("serial", form.getSerial());
            profile.put("category", form.getCategory());
            profile.put("type", form.getType());
            profile.put("haveCode", form.getHaveCode());
            invoice.setIssueProfileSnapshotJson(assets.json(profile));
        }
    }

    public static FormInvoiceEntity frozenForm(InvoiceEntity invoice, FormInvoiceEntity current) {
        if (invoice == null || invoice.getIssueProfileSnapshotJson() == null) return current;
        try {
            var profile = new ObjectMapper().readTree(invoice.getIssueProfileSnapshotJson());
            var result = new FormInvoiceEntity();
            result.setId(current == null ? null : current.getId());
            result.setCompanyId(invoice.getCompanyId().longValue());
            result.setSystem(0);
            if (current != null) {
                result.setPhoto(current.getPhoto());
                result.setName(current.getName());
                result.setStatus(current.getStatus());
            }
            result.setFormCode(profile.path("formCode").asText());
            result.setSerial(profile.path("serial").asText());
            result.setCategory(profile.path("category").asInt());
            result.setType(profile.path("type").asInt());
            result.setHaveCode(profile.path("haveCode").asInt());
            return result;
        } catch (Exception ex) { throw new IllegalStateException("Cấu hình phát hành đã chốt không hợp lệ", ex); }
    }

    private static void requireTaxMode(InvoiceTemplateVersionEntity version, FormInvoiceEntity form) {
        String expected = Objects.equals(form.getType(), 2) ? "MULTI_RATE" : "SINGLE_RATE";
        try {
            boolean compatible = false;
            for (var mode : new ObjectMapper().readTree(version.getCapabilitiesJson())) compatible |= expected.equals(mode.asText());
            if (!compatible) throw TemplateException.invalid("TEMPLATE_INCOMPATIBLE", "Mẫu không hỗ trợ chế độ thuế suất của cấu hình phát hành");
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException(ex); }
    }
}
