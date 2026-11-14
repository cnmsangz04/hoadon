package vn.hoadon.invoice.template.service;

import vn.hoadon.invoice.template.persistence.InvoiceTemplateEntity;
import java.util.Objects;

public record TemplateAccess(Long companyId, Long actorId, boolean systemManager) {
    public void requireRead(InvoiceTemplateEntity template) {
        if (!"SYSTEM".equals(template.getScope()) && !Objects.equals(companyId, template.getCompanyId())) {
            throw TemplateException.missing();
        }
    }

    public void requireWrite(InvoiceTemplateEntity template) {
        requireRead(template);
        if ("SYSTEM".equals(template.getScope()) && !systemManager) throw TemplateException.missing();
    }

    public void requireCompany() {
        if (companyId == null || actorId == null) throw TemplateException.missing();
    }

    public void requireSystemManager() {
        if (!systemManager || actorId == null) throw TemplateException.missing();
    }
}
