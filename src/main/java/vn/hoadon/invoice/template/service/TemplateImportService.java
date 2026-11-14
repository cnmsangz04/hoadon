package vn.hoadon.invoice.template.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.invoice.template.compiler.JsonLayoutCompiler;
import vn.hoadon.invoice.template.persistence.*;
import vn.hoadon.invoice.template.storage.InvoiceAssetService;

import java.time.*;
import java.util.*;

@Service
public class TemplateImportService {
    private final InvoiceTemplateImportJobRepository jobs;
    private final InvoiceAssetService assets;
    private final InvoiceTemplateService templates;
    private final JsonLayoutCompiler compiler;

    public TemplateImportService(InvoiceTemplateImportJobRepository jobs, InvoiceAssetService assets, InvoiceTemplateService templates, JsonLayoutCompiler compiler) {
        this.jobs = jobs;
        this.assets = assets;
        this.templates = templates;
        this.compiler = compiler;
    }

    @Transactional
    public InvoiceTemplateImportJobEntity create(Long sourceAssetId, Long targetTemplateId, Long expectedLockVersion, TemplateAccess access) {
        access.requireCompany();
        var source = assets.require(sourceAssetId, "COMPANY", access.companyId());
        if (!"COMPANY".equals(source.getScope()) || !"PDF_REFERENCE".equals(source.getKind())) throw TemplateException.invalid("INVALID_PDF_SOURCE", "Nguồn nhập phải là PDF của công ty");
        if (targetTemplateId != null) {
            templates.require(targetTemplateId, access, true);
            if (!Objects.equals(templates.draft(targetTemplateId).getLockVersion(), expectedLockVersion)) throw TemplateException.conflict("Bản nháp đã thay đổi");
        }
        var job = new InvoiceTemplateImportJobEntity();
        job.setCompanyId(access.companyId());
        job.setCreatedBy(access.actorId());
        job.setSourceAssetId(sourceAssetId);
        job.setTargetTemplateId(targetTemplateId);
        job.setExpectedDraftLockVersion(expectedLockVersion);
        job.setState("NOT_CONFIGURED");
        job.setErrorCode("AI_NOT_CONFIGURED");
        job.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        job = jobs.saveAndFlush(job);
        assets.pin(assets.json(Map.of("source", assets.reference(source))), "IMPORT_JOB", job.getId());
        return job;
    }

    public InvoiceTemplateImportJobEntity get(Long id, TemplateAccess access) {
        var job = jobs.findById(id).orElseThrow(TemplateException::missing);
        if (!Objects.equals(job.getCompanyId(), access.companyId())) throw TemplateException.missing();
        return job;
    }

    @Transactional
    public void acceptProposal(Long id, TemplateProposalProvider.Proposal proposal, TemplateAccess access) {
        var job = jobs.lockById(id).orElseThrow(TemplateException::missing);
        if (!Objects.equals(job.getCompanyId(), access.companyId())) throw TemplateException.missing();
        if (!Set.of("QUEUED", "RUNNING").contains(job.getState())) throw TemplateException.conflict("Tác vụ không ở trạng thái nhận kết quả");
        compiler.validate(proposal.layout(), false);
        job.setProvider(proposal.provider());
        job.setModel(proposal.model());
        job.setProposalJson(assets.json(Map.of("schemaVersion", "pdf-template-proposal/1", "layout", proposal.layout(), "mappings", proposal.mappings(), "warnings", proposal.warnings())));
        job.setErrorCode(null);
        job.setState("REVIEW_REQUIRED");
    }

    @Transactional
    public InvoiceTemplateDraftEntity apply(Long id, String name, int category, TemplateAccess access) {
        var job = jobs.lockById(id).orElseThrow(TemplateException::missing);
        if (!Objects.equals(job.getCompanyId(), access.companyId())) throw TemplateException.missing();
        if ("APPLIED".equals(job.getState())) {
            var target = templates.require(job.getTargetTemplateId(), access, true);
            return templates.draft(target.getId());
        }
        if (!"REVIEW_REQUIRED".equals(job.getState())) throw TemplateException.conflict("Chưa có đề xuất được xử lý để áp dụng");
        JsonNode proposal = templates.tree(job.getProposalJson());
        if (job.getTargetTemplateId() == null) {
            var created = templates.create(name, category, null, false, "pdf-import-" + id, access);
            job.setTargetTemplateId(created.template().getId());
            job.setExpectedDraftLockVersion(created.draft().getLockVersion());
        }
        var draft = templates.save(job.getTargetTemplateId(), job.getExpectedDraftLockVersion(), proposal.path("layout"), access);
        job.setState("APPLIED");
        job.setAppliedDraftId(draft.getId());
        return draft;
    }
}
