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
    private final vn.hoadon.repositories.InvoiceRepository invoices;

    public InvoiceTemplateBindingService(FormInvoiceRepository forms, InvoiceTemplateVersionRepository versions, InvoiceTemplateRepository templates,
                                          InvoiceFormTemplateService formTemplates, InvoiceAssetService assets,
                                          CompanyInvoiceDefaultRepository defaults, CompanyRepository companies,
                                          vn.hoadon.repositories.InvoiceRepository invoices) {
        this.forms = forms;
        this.versions = versions;
        this.templates = templates;
        this.formTemplates = formTemplates;
        this.assets = assets;
        this.defaults = defaults;
        this.companies = companies;
        this.invoices = invoices;
    }

    @Transactional
    public FormInvoiceEntity activeForm(Long companyId, int category) {
        companies.lockById(companyId).orElseThrow(TemplateException::missing);
        var selection = defaults.findByCompanyIdAndCategory(companyId, category).orElse(null);
        if (selection == null) {
            var candidates = forms.findByCompanyIdAndSystemAndCategoryAndStatus(companyId, 1, category, 1);
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
        if (!Objects.equals(form.getCompanyId(), companyId) || !Objects.equals(form.getCategory(), category) || !Objects.equals(form.getSystem(), 1) || !Objects.equals(form.getStatus(), 1)) throw TemplateException.invalid("DEFAULT_FORM_INVALID", "Mẫu mặc định không còn phù hợp; vui lòng chọn lại");
        return form;
    }

    @Transactional
    public void bindForm(Long formId, Long versionId, TemplateAccess access) {
        var existing = forms.findById(formId).orElseThrow(TemplateException::missing);
        if (access.systemManager() ? !Objects.equals(existing.getSystem(), 0)
                : !Objects.equals(existing.getSystem(), 1) || !Objects.equals(existing.getCompanyId(), access.companyId())) throw TemplateException.missing();
        if (invoices.existsByFormIdAndTemplateVersionIdIsNull(Math.toIntExact(formId))) throw TemplateException.invalid("INVOICE_TEMPLATE_NOT_PINNED", "Còn hóa đơn chưa có phiên bản mẫu. Cần hoàn tất chuyển dữ liệu trước khi đổi thiết kế.");
        var form = forms.lockById(formId).orElseThrow(TemplateException::missing);
        if (access.systemManager()) {
            if (!Objects.equals(form.getSystem(), 0)) throw TemplateException.missing();
        } else if (!Objects.equals(form.getCompanyId(), access.companyId()) || !Objects.equals(form.getSystem(), 1)) throw TemplateException.missing();
        var version = versions.findById(versionId).orElseThrow(TemplateException::missing);
        var template = templates.findById(version.getTemplateId()).orElseThrow(TemplateException::missing);
        access.requireRead(template);
        if (access.systemManager() && !"SYSTEM".equals(template.getScope())) throw TemplateException.missing();
        if (!Objects.equals(template.getCategory(), form.getCategory()) || !"ACTIVE".equals(template.getState())) throw TemplateException.invalid("TEMPLATE_INCOMPATIBLE", "Mẫu không phù hợp loại hóa đơn hoặc đã ngừng sử dụng");
        requireTaxMode(version, form);
        form.setTemplateVersionId(versionId);
        form.setTemplateId("SYSTEM".equals(template.getScope()) && !access.systemManager() ? null : template.getId());
    }

    @Transactional
    public void pin(InvoiceEntity invoice) {
        var form = forms.findById(invoice.getFormId().longValue()).orElseThrow(TemplateException::missing);
        Long companyId = invoice.getCompanyId().longValue();
        if (!Objects.equals(form.getSystem(), 1) || !Objects.equals(form.getCompanyId(), companyId)) throw TemplateException.missing();
        if (invoice.getTemplateVersionId() == null) {
            var version = formTemplates.published(form.getId());
            var template = templates.findById(version.getTemplateId()).orElseThrow(TemplateException::missing);
            new TemplateAccess(companyId, null, false).requireRead(template);
            requireTaxMode(version, form);
            invoice.setTemplateVersionId(version.getId());
        }
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
            result.setSystem(1);
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
