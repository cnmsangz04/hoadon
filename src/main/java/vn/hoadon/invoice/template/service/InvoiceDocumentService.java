package vn.hoadon.invoice.template.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.entity.*;
import vn.hoadon.invoice.template.catalog.*;
import vn.hoadon.invoice.template.persistence.*;
import vn.hoadon.invoice.template.render.TemplateRenderService;
import vn.hoadon.invoice.template.storage.*;
import vn.hoadon.repositories.*;
import vn.hoadon.services.SignatureAuthoritiesTaxService;
import vn.hoadon.util.InvoiceXmlBuilder;

import java.time.*;
import java.util.*;

@Service
public class InvoiceDocumentService {
    private final InvoiceRepository invoices;
    private final FormInvoiceRepository forms;
    private final CompanyRepository companies;
    private final CompanyBankRepository banks;
    private final SignatureVatRepository signatures;
    private final SignatureAuthoritiesTaxService authoritySignatures;
    private final InvoiceTemplateRepository templates;
    private final InvoiceTemplateVersionRepository versions;
    private final InvoiceDocumentSnapshotRepository snapshots;
    private final InvoiceFormTemplateService formTemplates;
    private final TemplateRenderService renders;
    private final InvoiceAssetService assets;
    private final InvoiceFixtureFactory fixtures;
    private final InvoiceFieldCatalog fieldCatalog;
    private final InvoiceExternalFieldRegistry externalFields;

    public InvoiceDocumentService(InvoiceRepository invoices, FormInvoiceRepository forms, CompanyRepository companies, CompanyBankRepository banks,
                                   SignatureVatRepository signatures, SignatureAuthoritiesTaxService authoritySignatures, InvoiceTemplateRepository templates,
                                   InvoiceTemplateVersionRepository versions, InvoiceDocumentSnapshotRepository snapshots, InvoiceFormTemplateService formTemplates,
                                   TemplateRenderService renders, InvoiceAssetService assets, InvoiceFixtureFactory fixtures,
                                   InvoiceFieldCatalog fieldCatalog, InvoiceExternalFieldRegistry externalFields) {
        this.invoices = invoices;
        this.forms = forms;
        this.companies = companies;
        this.banks = banks;
        this.signatures = signatures;
        this.authoritySignatures = authoritySignatures;
        this.templates = templates;
        this.versions = versions;
        this.snapshots = snapshots;
        this.formTemplates = formTemplates;
        this.renders = renders;
        this.assets = assets;
        this.fixtures = fixtures;
        this.fieldCatalog = fieldCatalog;
        this.externalFields = externalFields;
    }

    @Transactional
    public InvoiceRenderJobEntity sample(Long formId, boolean publicSample, TemplateAccess access) {
        var form = requireSampleForm(formId, publicSample, access);
        var version = formTemplates.published(formId);
        String xml = sampleXml(form, publicSample, access);
        String scope = publicSample ? "SYSTEM" : "COMPANY";
        return renders.enqueue(scope, publicSample ? null : access.companyId(), "TEMPLATE_SAMPLE", version.getId(), null, null, null, version.getSourceContent(), version.getManifestJson(), xml,
                Map.of("sampleMarker", "SAMPLE", "status", "Dữ liệu minh họa", "lookup_code", "SAMPLE0001",
                        "code_cqt", "MCCQT-MINH-HOA", "formId", formId.toString(), "invoiceCategory", String.valueOf(form.getCategory()), "taxMode", Objects.equals(form.getType(), 2) ? "MULTI_RATE" : "SINGLE_RATE", "sourceStage", "DRAFT"));
    }

    @Transactional(readOnly = true)
    public String sampleXml(Long formId, boolean publicSample, TemplateAccess access) {
        return sampleXml(requireSampleForm(formId, publicSample, access), publicSample, access);
    }

    private FormInvoiceEntity requireSampleForm(Long formId, boolean publicSample, TemplateAccess access) {
        var form = forms.findById(formId).orElseThrow(TemplateException::missing);
        if (publicSample) {
            if (!Objects.equals(form.getSystem(), 0) || !Objects.equals(form.getStatus(), 1) && !access.systemManager()) throw TemplateException.missing();
        } else if (!Objects.equals(form.getCompanyId(), access.companyId()) || !Objects.equals(form.getSystem(), 1)) throw TemplateException.missing();
        return form;
    }

    private String sampleXml(FormInvoiceEntity form, boolean publicSample, TemplateAccess access) {
        CompanyEntity company = publicSample ? null : companies.findById(access.companyId()).orElse(null);
        CompanyBankEntity bank = company == null ? null : banks.findByCompany(company).stream().findFirst().orElse(null);
        return fixtures.sample(form, company, bank, 3);
    }

    @Transactional
    public InvoiceRenderJobEntity invoice(Long invoiceId) {
        var invoice = invoices.lockDocumentById(invoiceId).orElseThrow(TemplateException::missing);
        Long companyId = invoice.getCompanyId().longValue();
        var form = forms.findById(invoice.getFormId().longValue()).orElseThrow(TemplateException::missing);
        if (!Objects.equals(form.getSystem(), 0) && !Objects.equals(form.getCompanyId(), companyId)) throw TemplateException.missing();
        if (invoice.getTemplateVersionId() == null) throw TemplateException.invalid("INVOICE_TEMPLATE_NOT_PINNED", "Hóa đơn chưa có phiên bản mẫu. Cần hoàn tất chuyển dữ liệu trước khi xem bản in.");
        var version = versions.findById(invoice.getTemplateVersionId()).orElseThrow(TemplateException::missing);
        var template = templates.findById(version.getTemplateId()).orElseThrow(TemplateException::missing);
        new TemplateAccess(companyId, null, false).requireRead(template);
        var source = xml(invoice, form);
        String hash = DocumentStorage.sha256(source.content());
        Map<String, String> context = new TreeMap<>();
        context.put("sourceStage", source.stage());
        context.put("lookupCode", Objects.toString(invoice.getLookupCode(), ""));
        context.put("lookup_code", Objects.toString(invoice.getLookupCode(), ""));
        context.put("code_cqt", Objects.toString(invoice.getCodeCqt(), ""));
        context.put("authorityCode", Objects.toString(invoice.getCodeCqt(), ""));
        context.put("statusLabel", String.valueOf(invoice.getStatus()));
        context.put("status", String.valueOf(invoice.getStatus()));
        var catalogSnapshot = fieldCatalog.snapshot(version.getFieldCatalogVersion());
        context.putAll(externalFields.capture(invoice, catalogSnapshot));
        context.put("invoiceCategory", String.valueOf(template.getCategory()));
        context.put("taxMode", Objects.equals(form.getType(), 2) ? "MULTI_RATE" : "SINGLE_RATE");
        context.put("catalogCode", catalogSnapshot.code());
        String snapshotHash = DocumentStorage.sha256(hash + assets.json(context));
        var snapshot = snapshots.findTopByInvoiceIdAndTemplateVersionIdAndSourceStageAndSourceHashOrderByIdDesc(invoiceId, version.getId(), source.stage(), snapshotHash).orElse(null);
        if (snapshot == null) {
            snapshot = new InvoiceDocumentSnapshotEntity();
            snapshot.setInvoiceId(invoiceId);
            snapshot.setCompanyId(companyId);
            snapshot.setTemplateVersionId(version.getId());
            snapshot.setSourceStage(source.stage());
            snapshot.setSourceXml(source.content());
            snapshot.setSourceHash(snapshotHash);
            snapshot.setContextJson(assets.json(context));
            snapshot.setFidelity(snapshots.findTopByInvoiceIdOrderByIdAsc(invoiceId).map(InvoiceDocumentSnapshotEntity::getFidelity)
                    .orElse("PINNED_VERSION"));
            snapshot.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            snapshot = snapshots.saveAndFlush(snapshot);
            assets.pin(version.getManifestJson(), "SNAPSHOT", snapshot.getId());
        }
        return renders.enqueue("COMPANY", companyId, "INVOICE", version.getId(), null, null, snapshot.getId(), version.getSourceContent(), version.getManifestJson(), snapshot.getSourceXml(), context);
    }

    public record XmlSource(String stage, String content) { }

    public XmlSource xml(InvoiceEntity invoice, FormInvoiceEntity form) {
        int status = invoice.getStatus() == null ? 0 : invoice.getStatus();
        if (status > 2) {
            var authority = authoritySignatures.getLatestByInvoiceIdAndCompanyId(Math.toIntExact(invoice.getId()), invoice.getCompanyId());
            if (authority != null && authority.xml != null && !authority.xml.isBlank()) return verified(invoice, "AUTHORITY", authority.xml);
        }
        if (status > 0) {
            var signed = signatures.findTopByInvoiceIdAndCompanyIdOrderByIdDesc(Math.toIntExact(invoice.getId()), invoice.getCompanyId()).orElse(null);
            if (signed != null && signed.getXml() != null && !signed.getXml().isBlank()) return verified(invoice, "SIGNED", signed.getXml());
            throw TemplateException.invalid("SOURCE_XML_MISSING", "Không tìm thấy XML đã ký của hóa đơn; cần đối chiếu dữ liệu nguồn");
        }
        var company = companies.findById(invoice.getCompanyId().longValue()).orElseThrow(TemplateException::missing);
        var bank = banks.findByCompany(company).stream().findFirst().orElse(null);
        var reference = invoice.getReferenceId() == null ? null : invoices.findById(invoice.getReferenceId().longValue()).orElse(null);
        if (reference != null && !Objects.equals(reference.getCompanyId(), invoice.getCompanyId())) throw TemplateException.missing();
        var referenceForm = reference == null ? null : forms.findById(reference.getFormId().longValue()).orElse(null);
        return new XmlSource("DRAFT", InvoiceXmlBuilder.build(invoice, form, company, bank, reference, referenceForm));
    }

    private static XmlSource verified(InvoiceEntity invoice, String stage, String xml) {
        var document = InvoiceViewModelMapper.parse(xml);
        var nodes = document.getElementsByTagNameNS("*", "DLHDon");
        if (nodes.getLength() != 1) throw TemplateException.invalid("SOURCE_XML_MISMATCH", "XML không chứa đúng một hóa đơn");
        var data = (org.w3c.dom.Element) nodes.item(0);
        if (invoice.getIdAttr() != null && !invoice.getIdAttr().equals(data.getAttribute("Id"))) throw TemplateException.invalid("SOURCE_XML_MISMATCH", "XML đã ký không khớp định danh hóa đơn");
        return new XmlSource(stage, xml);
    }
}
