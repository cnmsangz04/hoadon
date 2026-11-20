package vn.hoadon.invoice.template.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.criteria.Root;
import vn.hoadon.invoice.template.catalog.*;
import vn.hoadon.invoice.template.compiler.JsonLayoutCompiler;
import vn.hoadon.invoice.template.config.InvoiceTemplateProperties;
import vn.hoadon.invoice.template.persistence.*;
import vn.hoadon.invoice.template.render.TemplateRenderService;
import vn.hoadon.invoice.template.storage.*;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.CompanyBankEntity;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.repositories.CompanyBankRepository;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.FormInvoiceRepository;

import java.time.*;
import java.util.*;

@Service
public class InvoiceTemplateService {
    public record Details(InvoiceTemplateEntity template, InvoiceTemplateDraftEntity draft, List<InvoiceTemplateVersionEntity> versions) { }
    private final InvoiceTemplateRepository templates;
    private final InvoiceTemplateDraftRepository drafts;
    private final InvoiceTemplateVersionRepository versions;
    private final InvoiceTemplateVersionAssetRepository versionAssets;
    private final InvoiceTemplateValidationRunRepository validations;
    private final InvoiceTemplateAuditRepository audit;
    private final InvoiceAssetService assets;
    private final TemplateRenderService renders;
    private final InvoiceFixtureFactory fixtures;
    private final JsonLayoutCompiler compiler;
    private final InvoiceFieldCatalog catalog;
    private final InvoiceTemplateProperties properties;
    private final ObjectMapper mapper;
    private final ObjectProvider<InvoiceTemplateThumbnailService> thumbnails;
    private final FormInvoiceRepository forms;
    private final CompanyRepository companies;
    private final CompanyBankRepository banks;
    private final InvoiceTemplateHistoryService history;
    private final InvoiceAssetPinRepository pins;
    private final InvoiceTemplateStorageCleanupService storageCleanup;

    public InvoiceTemplateService(InvoiceTemplateRepository templates, InvoiceTemplateDraftRepository drafts,
                                   InvoiceTemplateVersionRepository versions, InvoiceTemplateVersionAssetRepository versionAssets,
                                   InvoiceTemplateValidationRunRepository validations, InvoiceTemplateAuditRepository audit,
                                   InvoiceAssetService assets, TemplateRenderService renders, InvoiceFixtureFactory fixtures,
                                   JsonLayoutCompiler compiler, InvoiceFieldCatalog catalog,
                                   InvoiceTemplateProperties properties, ObjectMapper mapper,
                                   ObjectProvider<InvoiceTemplateThumbnailService> thumbnails,
                                   FormInvoiceRepository forms, CompanyRepository companies, CompanyBankRepository banks,
                                   InvoiceTemplateHistoryService history,
                                   InvoiceAssetPinRepository pins,
                                   InvoiceTemplateStorageCleanupService storageCleanup) {
        this.templates = templates;
        this.drafts = drafts;
        this.versions = versions;
        this.versionAssets = versionAssets;
        this.validations = validations;
        this.audit = audit;
        this.assets = assets;
        this.renders = renders;
        this.fixtures = fixtures;
        this.compiler = compiler;
        this.catalog = catalog;
        this.properties = properties;
        this.mapper = mapper;
        this.thumbnails = thumbnails;
        this.forms = forms;
        this.companies = companies;
        this.banks = banks;
        this.history = history;
        this.pins = pins;
        this.storageCleanup = storageCleanup;
    }

    public Page<InvoiceTemplateEntity> list(TemplateAccess access, String scope, String q, Integer category, int page, int size) {
        Specification<InvoiceTemplateEntity> filter = (root, query, cb) -> {
            jakarta.persistence.criteria.Predicate owner;
            if ("SYSTEM".equals(scope)) {
                owner = cb.equal(root.get("scope"), "SYSTEM");
            } else if ("COMPANY_AND_SYSTEM".equals(scope)) {
                owner = cb.or(
                        cb.equal(root.get("scope"), "SYSTEM"),
                        cb.and(cb.equal(root.get("scope"), "COMPANY"), cb.equal(root.get("companyId"), access.companyId()))
                );
            } else {
                owner = cb.and(cb.equal(root.get("scope"), "COMPANY"), cb.equal(root.get("companyId"), access.companyId()));
            }
            var predicate = cb.and(
                    owner,
                    cb.isNull(root.get("sourceVersionId")),
                    activeFormLink(root, query, cb, access, "SYSTEM".equals(scope))
            );
            if (category != null) predicate = cb.and(predicate, cb.equal(root.get("category"), category));
            if (q != null && !q.isBlank()) predicate = cb.and(predicate, cb.like(cb.lower(root.get("name")), "%" + q.trim().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%", '\\'));
            return predicate;
        };
        return templates.findAll(filter, PageRequest.of(Math.max(0, page - 1), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "updatedAt", "id")));
    }

    private jakarta.persistence.criteria.Predicate activeFormLink(
            Root<InvoiceTemplateEntity> template,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            TemplateAccess access,
            boolean systemOnly) {
        var formsQuery = query.subquery(Long.class);
        var form = formsQuery.from(FormInvoiceEntity.class);
        jakarta.persistence.criteria.Predicate owner;
        if (systemOnly) {
            owner = cb.and(cb.equal(form.get("system"), 1), cb.equal(form.get("status"), 1));
        } else {
            var companyForm = cb.and(
                    cb.equal(form.get("system"), 0),
                    cb.equal(form.get("companyId"), access.companyId()),
                    form.get("status").in(0, 1)
            );
            var systemForm = cb.and(
                    cb.equal(form.get("system"), 1),
                    cb.equal(form.get("status"), 1)
            );
            owner = cb.or(companyForm, systemForm);
        }

        var versionIds = query.subquery(Long.class);
        var version = versionIds.from(InvoiceTemplateVersionEntity.class);
        versionIds.select(version.get("id"));
        versionIds.where(cb.equal(version.get("templateId"), template.get("id")));

        var linked = cb.or(
                cb.equal(form.get("templateId"), template.get("id")),
                form.get("templateVersionId").in(versionIds)
        );
        formsQuery.select(form.get("id"));
        formsQuery.where(cb.and(owner, linked));
        return cb.exists(formsQuery);
    }

    @Transactional
    public Details details(Long id, TemplateAccess access) {
        var template = require(id, access, false);
        var editable = !"SYSTEM".equals(template.getScope()) || access.systemManager();
        var draft = editable ? drafts.findByTemplateId(id).orElse(null) : null;
        if (editable && draft == null) {
            var version = versions.findTopByTemplateIdOrderByVersionNoDesc(id).orElse(null);
            if (version != null) draft = createDraftFromVersion(template, version, access.actorId());
        }
        return new Details(template, draft, versions.findTop3ByTemplateIdOrderByVersionNoDesc(id));
    }

    @Transactional
    public Details ensureDraft(Long id, TemplateAccess access) {
        var template = lock(id, access);
        ensureEditableState(template);
        var draft = drafts.findByTemplateId(id).orElse(null);
        if (draft == null) {
            var version = versions.findTopByTemplateIdOrderByVersionNoDesc(id)
                    .orElseThrow(() -> TemplateException.invalid("TEMPLATE_DRAFT_REQUIRED", "Mẫu chưa có phiên bản để tạo bản nháp"));
            draft = createDraftFromVersion(template, version, access.actorId());
        }
        return new Details(template, draft, versions.findTop3ByTemplateIdOrderByVersionNoDesc(id));
    }

    @Transactional
    public void trimHistory(Long id, TemplateAccess access) {
        lock(id, access);
        history.trim(id);
    }

    @Transactional
    public void deleteOwned(Long id, Long formId, TemplateAccess access) {
        var template = lock(id, access);
        var links = forms.findByTemplateId(id);
        if (links.size() != 1 || !Objects.equals(links.getFirst().getId(), formId)) {
            throw TemplateException.conflict("Mỗi template chỉ được thuộc về một mẫu hóa đơn");
        }
        var form = links.getFirst();
        var draft = drafts.findByTemplateId(id).orElse(null);
        var storageSnapshot = storageCleanup.snapshot(id, draft == null ? null : draft.getId());
        form.setTemplateVersionId(null);
        form.setTemplateId(null);
        forms.saveAndFlush(form);
        history.deleteAll(id);
        if (draft != null) {
            validations.deleteByDraftId(draft.getId());
            pins.deleteByOwnerTypeAndOwnerId("DRAFT", draft.getId());
            drafts.delete(draft);
        }
        audit.deleteByTemplateId(id);
        storageCleanup.removeDatabaseRows(storageSnapshot);
        templates.delete(template);
        templates.flush();
        storageCleanup.deleteFiles(storageSnapshot);
    }

    @Transactional
    public Details deleteVersion(Long id, Long versionId, TemplateAccess access) {
        var template = lock(id, access);
        history.delete(id, versionId);
        template.setUpdatedAt(now());
        event(template, access, "DELETE_VERSION", null, null, versionId, "{}");
        return details(id, access);
    }

    private void ensureEditableState(InvoiceTemplateEntity template) {
        if ("ACTIVE".equals(template.getState())) return;
        boolean linkedToForm = forms.findByTemplateId(template.getId()).stream()
                .anyMatch(form -> Objects.equals(form.getStatus(), 0) || Objects.equals(form.getStatus(), 1));
        if (!linkedToForm) throw TemplateException.conflict("Mẫu đã ngừng sử dụng");
    }

    @Transactional
    public Details create(String name, int category, Long sourceVersionId, boolean system, String key, TemplateAccess access, JsonNode initialLayout) {
        if (system) access.requireSystemManager(); else access.requireCompany();
        validateName(name, category);
        String requestKey = commandKey(access, "CREATE", key);
        String requestHash = DocumentStorage.sha256(assets.json(Arrays.asList(name, category, sourceVersionId, system, initialLayout)));
        var previous = audit.findByRequestKey(requestKey);
        if (previous.isPresent()) {
            verifyRequest(previous.get(), requestHash);
            return details(previous.get().getResultId(), access);
        }
        InvoiceTemplateVersionEntity source = sourceVersionId == null ? null : requireVersion(sourceVersionId, access);
        if (source != null) {
            var parent = require(source.getTemplateId(), access, false);
            if (parent.getCategory() != category || !"ACTIVE".equals(parent.getState())) throw TemplateException.invalid("TEMPLATE_INCOMPATIBLE", "Mẫu nguồn không phù hợp hoặc đã ngừng sử dụng");
        } else if (initialLayout == null) {
            throw TemplateException.invalid("LAYOUT_REQUIRED", "Phải tải file JSON bố cục trước khi tạo mẫu");
        } else {
            initialLayout = normalizeCatalogVersion(initialLayout);
            compiler.validate(initialLayout, false);
        }
        var template = new InvoiceTemplateEntity();
        template.setScope(system ? "SYSTEM" : "COMPANY");
        template.setCompanyId(system ? null : access.companyId());
        template.setName(name.trim());
        template.setCategory(category);
        template.setState("ACTIVE");
        template.setSourceVersionId(sourceVersionId);
        template.setCreatedBy(access.actorId());
        template.setUpdatedBy(access.actorId());
        template.setCreatedAt(now());
        template.setUpdatedAt(now());
        template = templates.saveAndFlush(template);
        String sourceContent;
        if (source != null) {
            sourceContent = source.getSourceContent();
        } else {
            sourceContent = assets.json(initialLayout);
        }
        String manifestJson = source == null ? "{}" : source.getManifestJson();
        var draft = new InvoiceTemplateDraftEntity();
        draft.setTemplateId(template.getId());
        draft.setSourceContent(sourceContent);
        draft.setManifestJson(manifestJson);
        assets.resources(draft.getManifestJson(), template.getScope(), template.getCompanyId());
        draft.setSchemaVersion(JsonLayoutCompiler.SCHEMA_VERSION);
        draft.setFieldCatalogVersion(tree(sourceContent).path("fieldCatalogVersion").asText());
        draft.setContentHash(hash(draft.getSourceContent(), draft.getManifestJson()));
        draft.setUpdatedBy(access.actorId());
        draft.setUpdatedAt(now());
        draft = drafts.saveAndFlush(draft);

        assets.pin(draft.getManifestJson(), "DRAFT", draft.getId());
        event(template, access, "CREATE", requestKey, requestHash, template.getId(), "{}");
        return new Details(template, draft, List.of());
    }

    @Transactional
    public Details clonePublished(String name, int category, Long sourceVersionId,
                                  boolean system, String key, TemplateAccess access) {
        if (sourceVersionId == null) throw TemplateException.invalid("TEMPLATE_SOURCE_REQUIRED", "Thiếu phiên bản mẫu nguồn");
        var details = create(name, category, sourceVersionId, system, key, access, null);
        if (!details.versions().isEmpty()) return details;

        var source = requireVersion(sourceVersionId, access);
        var sourceTemplate = require(source.getTemplateId(), access, false);
        if (!"ACTIVE".equals(sourceTemplate.getState()) || sourceTemplate.getCategory() != category) {
            throw TemplateException.invalid("TEMPLATE_INCOMPATIBLE", "Mẫu nguồn không phù hợp hoặc đã ngừng sử dụng");
        }
        var version = copyPublishedVersion(details.template(), source, access.actorId());
        var draft = details.draft();
        draft.setBaseVersionId(version.getId());
        draft.setUpdatedAt(now());
        draft.setUpdatedBy(access.actorId());
        drafts.saveAndFlush(draft);
        return new Details(details.template(), draft, List.of(version));
    }

    @Transactional
    public InvoiceTemplateDraftEntity save(Long id, long expectedVersion, JsonNode layout, TemplateAccess access) {
        var template = lock(id, access);
        ensureEditableState(template);
        var draft = draft(id);
        checkRevision(draft, expectedVersion);
        layout = normalizeCatalogVersion(layout);
        if (!draft.getFieldCatalogVersion().equals(layout.path("fieldCatalogVersion").asText())) throw TemplateException.conflict("Danh mục phần tử đã thay đổi; vui lòng mở lại mẫu để cập nhật");
        return saveDraft(template, draft, layout, access);
    }

    private JsonNode normalizeCatalogVersion(JsonNode layout) {
        if (layout == null || !"CURRENT".equalsIgnoreCase(layout.path("fieldCatalogVersion").asText())) return layout;
        var normalized = (com.fasterxml.jackson.databind.node.ObjectNode) layout.deepCopy();
        normalized.put("fieldCatalogVersion", catalog.version());
        return normalized;
    }

    private InvoiceTemplateDraftEntity saveDraft(InvoiceTemplateEntity template, InvoiceTemplateDraftEntity draft, JsonNode layout, TemplateAccess access) {
        compiler.validate(layout, false);
        Map<String, InvoiceAssetService.AssetRef> manifest = layoutManifest(layout, template);
        draft.setFieldCatalogVersion(layout.path("fieldCatalogVersion").asText());
        draft.setSchemaVersion(JsonLayoutCompiler.SCHEMA_VERSION);
        draft.setSourceContent(assets.json(layout));
        draft.setManifestJson(assets.json(manifest));
        draft.setContentHash(hash(draft.getSourceContent(), draft.getManifestJson()));
        draft.setUpdatedBy(access.actorId());
        draft.setUpdatedAt(now());
        template.setUpdatedBy(access.actorId());
        template.setUpdatedAt(now());
        draft = drafts.saveAndFlush(draft);

        assets.pin(draft.getManifestJson(), "DRAFT", draft.getId());
        var savedDraft = draft;
        thumbnails.ifAvailable(service -> service.refresh(template, savedDraft));
        event(template, access, "SAVE_DRAFT", null, null, draft.getId(), "{}");
        return draft;
    }

    @Transactional
    public InvoiceTemplateVersionEntity saveInitialVersion(Long id, TemplateAccess access) {
        var template = lock(id, access);
        ensureEditableState(template);
        var draft = draft(id);
        var layout = tree(draft.getSourceContent());
        compiler.validate(layout, true);
        layoutManifest(layout, template);

        var version = freeze(template, draft, access.actorId(), assets.json(Map.of("mode", "INITIAL")));
        discardDraft(draft);
        event(template, access, "SAVE_INITIAL_VERSION", null, null, version.getId(), "{}");
        history.trim(id);
        return version;
    }

    public void refreshThumbnail(Details details) {
        thumbnails.ifAvailable(service -> service.refresh(details.template(), details.draft()));
    }

    @Transactional
    public InvoiceTemplateDraftEntity upgradeCatalog(Long id, long expectedVersion, TemplateAccess access) {
        var template = lock(id, access);
        ensureEditableState(template);
        var draft = draft(id);
        checkRevision(draft, expectedVersion);
        var layout = (com.fasterxml.jackson.databind.node.ObjectNode) tree(draft.getSourceContent());
        layout.put("fieldCatalogVersion", catalog.version());
        draft = saveDraft(template, draft, layout, access);
        event(template, access, "UPGRADE_CATALOG", null, null, draft.getId(), "{}");
        return draft;
    }

    public String designerFontCss() { return renders.designerFontCss(); }

    private String sampleXml(InvoiceTemplateEntity template, int rows) {
        FormInvoiceEntity form = forms.findByTemplateId(template.getId()).stream().findFirst().orElseGet(() -> {
            FormInvoiceEntity fallback = new FormInvoiceEntity();
            fallback.setCategory(template.getCategory());
            fallback.setType(template.getCategory() == 2 ? 0 : 1);
            fallback.setFormCode(String.valueOf(template.getCategory()));
            fallback.setSerial("C26TAA");
            return fallback;
        });
        CompanyEntity company = null;
        CompanyBankEntity bank = null;
        if ("COMPANY".equals(template.getScope()) && template.getCompanyId() != null) {
            company = companies.findById(template.getCompanyId()).orElse(null);
            if (company != null) bank = banks.findByCompany(company).stream().findFirst().orElse(null);
        }
        return fixtures.sample(form, company, bank, rows);
    }

    @Transactional(readOnly = true)
    public String designerPreview(Long id, JsonNode layout, int rows, TemplateAccess access) {
        var template = require(id, access, true);
        ensureEditableState(template);
        if (rows < 0 || rows > 60) throw TemplateException.invalid("INVALID_ROWS", "Số dòng minh họa phải từ 0 đến 60");
        if (assets.json(layout).getBytes(java.nio.charset.StandardCharsets.UTF_8).length > properties.getMaxInputBytes())
            throw TemplateException.invalid("RENDER_INPUT_LIMIT", "Nội dung bố cục vượt giới hạn");
        compiler.validate(layout, false);
        var manifest = layoutManifest(layout, template);
        return renders.designerPreview(layout, sampleXml(template, rows), assets.json(manifest), template.getScope(), template.getCompanyId());
    }

    private Map<String, InvoiceAssetService.AssetRef> layoutManifest(JsonNode layout, InvoiceTemplateEntity template) {
        Map<String, InvoiceAssetService.AssetRef> manifest = new TreeMap<>();
        layout.path("assets").fields().forEachRemaining(entry -> {
            var asset = assets.require(entry.getValue().path("assetId").asLong(), template.getScope(), template.getCompanyId());
            if (!Set.of("IMAGE", "FONT").contains(asset.getKind())) throw TemplateException.invalid("INVALID_ASSET_KIND", "Layout chỉ nhận ảnh và font");
            manifest.put(entry.getKey(), assets.reference(asset));
        });
        layout.path("fontFamilies").fields().forEachRemaining(entry -> entry.getValue().elements().forEachRemaining(ref -> {
            var value = manifest.get(ref.asText());
            if (value == null || !value.mimeType().startsWith("font/")) throw TemplateException.invalid("FONT_UNAVAILABLE", "Font family phải trỏ đến file font");
        }));
        return manifest;
    }

    @Transactional
    public InvoiceTemplateValidationRunEntity validate(Long id, long expectedVersion, TemplateAccess access) {
        var template = lock(id, access);
        var draft = draft(id);
        checkRevision(draft, expectedVersion);
        compiler.validate(tree(draft.getSourceContent()), true);
        for (var previous : validations.findByDraftIdOrderByIdDesc(draft.getId())) {
            if (!"RUNNING".equals(previous.getState())) {
                storageCleanup.deleteRenderJobs(jobIds(previous.getJobIdsJson()));
                validations.delete(previous);
            }
        }
        List<Long> jobIds = new ArrayList<>();
        for (int count : List.of(0, 1, 60)) jobIds.add(preview(template, draft, count, access.companyId()).getId());
        var run = new InvoiceTemplateValidationRunEntity();
        run.setDraftId(draft.getId());
        run.setDraftLockVersion(draft.getLockVersion());
        run.setContentHash(draft.getContentHash());
        run.setManifestHash(DocumentStorage.sha256(draft.getManifestJson()));
        run.setProfileVersion(properties.getProfileVersion());
        run.setJobIdsJson(assets.json(jobIds));
        run.setState("RUNNING");
        run.setReportJson("{}");
        run.setCreatedAt(now());
        return validations.save(run);
    }

    @Transactional
    public InvoiceTemplateValidationRunEntity validation(Long id, TemplateAccess access) {
        var run = validations.findById(id).orElseThrow(TemplateException::missing);
        var draft = drafts.findById(run.getDraftId()).orElseThrow(TemplateException::missing);
        require(draft.getTemplateId(), access, true);
        boolean ready = true;
        List<Map<String, Object>> results = new ArrayList<>();
        for (JsonNode value : tree(run.getJobIdsJson())) {
            var job = renders.job(value.asLong(), access);
            if ("FAILED".equals(job.getState())) {
                run.setState("FAILED");
                results.add(Map.of("jobId", job.getId(), "errorCode", job.getErrorCode()));
            } else if (!"SUCCEEDED".equals(job.getState())) ready = false;
            else results.add(Map.of("jobId", job.getId(), "artifactId", job.getArtifactId()));
        }
        if (ready && !"FAILED".equals(run.getState())) run.setState("PASSED");
        run.setReportJson(assets.json(Map.of("contentHash", run.getContentHash(), "profileVersion", run.getProfileVersion(), "fixtures", results,
                "displayPolicy", "invoice-base/1")));
        return run;
    }

    @Transactional
    public InvoiceTemplateVersionEntity publish(Long id, long expectedVersion, Long reportId, String key, TemplateAccess access) {
        var template = lock(id, access);
        String requestKey = commandKey(access, "PUBLISH:" + id, key);
        String requestHash = DocumentStorage.sha256(assets.json(List.of(id, expectedVersion, reportId)));
        var previous = audit.findByRequestKey(requestKey);
        if (previous.isPresent()) {
            verifyRequest(previous.get(), requestHash);
            return versions.findById(previous.get().getResultId()).orElseThrow(TemplateException::missing);
        }
        ensureEditableState(template);
        var draft = draft(id);
        checkRevision(draft, expectedVersion);
        var run = validation(reportId, access);
        if (!run.getDraftId().equals(draft.getId()) || !"PASSED".equals(run.getState()) || !run.getContentHash().equals(draft.getContentHash()) || !run.getDraftLockVersion().equals(draft.getLockVersion()) || !run.getManifestHash().equals(DocumentStorage.sha256(draft.getManifestJson())) || !run.getProfileVersion().equals(properties.getProfileVersion())) {
            throw TemplateException.conflict("Bản kiểm tra chưa đạt hoặc không khớp revision hiện tại");
        }
        var version = freeze(template, draft, access.actorId(), run.getReportJson());
        discardDraft(draft);
        template.setUpdatedAt(now());
        event(template, access, "PUBLISH", requestKey, requestHash, version.getId(), run.getReportJson());
        history.trim(id);
        storageCleanup.deleteRenderJobs(jobIds(run.getJobIdsJson()));
        validations.delete(run);
        return version;
    }

    @Transactional
    public InvoiceTemplateDraftEntity restore(Long id, Long versionId, long expectedVersion, TemplateAccess access) {
        var template = lock(id, access);
        ensureEditableState(template);
        var source = requireVersion(versionId, access);
        if (!source.getTemplateId().equals(id)) throw TemplateException.missing();
        var draft = draft(id);
        checkRevision(draft, expectedVersion);
        draft.setBaseVersionId(versionId);
        draft.setSourceContent(source.getSourceContent());
        draft.setSchemaVersion(source.getSchemaVersion());
        draft.setFieldCatalogVersion(source.getFieldCatalogVersion());
        draft.setManifestJson(source.getManifestJson());
        draft.setContentHash(source.getContentHash());
        draft.setUpdatedBy(access.actorId());
        draft.setUpdatedAt(now());
        event(template, access, "RESTORE_DRAFT", null, null, draft.getId(), assets.json(Map.of("versionId", versionId)));
        draft = drafts.saveAndFlush(draft);
        assets.pin(draft.getManifestJson(), "DRAFT", draft.getId());
        return draft;
    }

    private InvoiceTemplateDraftEntity createDraftFromVersion(InvoiceTemplateEntity template,
                                                               InvoiceTemplateVersionEntity version,
                                                               Long actorId) {
        var draft = new InvoiceTemplateDraftEntity();
        draft.setTemplateId(template.getId());
        draft.setBaseVersionId(version.getId());
        draft.setSourceContent(version.getSourceContent());
        draft.setManifestJson(version.getManifestJson());
        draft.setSchemaVersion(version.getSchemaVersion());
        draft.setFieldCatalogVersion(version.getFieldCatalogVersion());
        draft.setContentHash(version.getContentHash());
        draft.setUpdatedBy(actorId);
        draft.setUpdatedAt(now());
        draft = drafts.saveAndFlush(draft);
        assets.pin(draft.getManifestJson(), "DRAFT", draft.getId());
        event(template, new TemplateAccess(template.getCompanyId(), actorId, "SYSTEM".equals(template.getScope())),
                "CREATE_DRAFT", null, null, draft.getId(), assets.json(Map.of("versionId", version.getId())));
        return draft;
    }

    private void discardDraft(InvoiceTemplateDraftEntity draft) {
        storageCleanup.deleteDraftPreviewJobs(draft.getId());
        validations.deleteByDraftId(draft.getId());
        pins.deleteByOwnerTypeAndOwnerId("DRAFT", draft.getId());
        drafts.delete(draft);
        drafts.flush();
    }

    @Transactional
    public void archive(Long id, long expectedVersion, TemplateAccess access) {
        var template = lock(id, access);
        if (!Objects.equals(template.getLockVersion(), expectedVersion)) throw TemplateException.conflict("Mẫu đã được cập nhật");
        template.setState("ARCHIVED");
        template.setUpdatedAt(now());
        event(template, access, "ARCHIVE", null, null, id, "{}");
    }

    public InvoiceRenderJobEntity preview(Long id, long expectedVersion, int rows, TemplateAccess access) {
        var template = require(id, access, true);
        var draft = draft(id);
        checkRevision(draft, expectedVersion);
        return preview(template, draft, Math.clamp(rows, 0, 100), access.companyId());
    }

    private InvoiceRenderJobEntity preview(InvoiceTemplateEntity template, InvoiceTemplateDraftEntity draft, int rows, Long storageCompanyId) {
        return renders.enqueue(template.getScope(), storageCompanyId, "DRAFT_PREVIEW", null, draft.getId(), draft.getLockVersion(), null,
                draft.getSourceContent(), draft.getManifestJson(), sampleXml(template, rows),
                Map.of("sampleMarker", "SAMPLE", "status", "Dữ liệu minh họa", "lookup_code", "SAMPLE0001", "code_cqt", "MCCQT-MINH-HOA"));
    }

    public InvoiceTemplateVersionEntity requireVersion(Long id, TemplateAccess access) {
        var version = versions.findById(id).orElseThrow(TemplateException::missing);
        require(version.getTemplateId(), access, false);
        return version;
    }

    public InvoiceTemplateEntity require(Long id, TemplateAccess access, boolean write) {
        var template = templates.findById(id).orElseThrow(TemplateException::missing);
        if (write) access.requireWrite(template); else access.requireRead(template);
        return template;
    }

    public InvoiceTemplateDraftEntity draft(Long id) { return drafts.findByTemplateId(id).orElseThrow(TemplateException::missing); }

    private List<Long> jobIds(String json) {
        List<Long> ids = new ArrayList<>();
        for (JsonNode node : tree(json)) {
            if (node.canConvertToLong()) ids.add(node.asLong());
        }
        return ids;
    }


    private InvoiceTemplateEntity lock(Long id, TemplateAccess access) {
        var template = templates.lockById(id).orElseThrow(TemplateException::missing);
        access.requireWrite(template);
        return template;
    }

    private InvoiceTemplateVersionEntity freeze(InvoiceTemplateEntity template, InvoiceTemplateDraftEntity draft, Long actor, String report) {
        var version = new InvoiceTemplateVersionEntity();
        version.setTemplateId(template.getId());
        version.setVersionNo(versions.findTopByTemplateIdOrderByVersionNoDesc(template.getId()).map(v -> v.getVersionNo() + 1).orElse(1));
        version.setCapabilitiesJson(assets.json(List.of("SINGLE_RATE", "MULTI_RATE")));
        version.setSourceContent(draft.getSourceContent());
        version.setManifestJson(draft.getManifestJson());
        version.setSchemaVersion(draft.getSchemaVersion());
        version.setFieldCatalogVersion(draft.getFieldCatalogVersion());
        version.setCompilerVersion(JsonLayoutCompiler.COMPILER_VERSION);
        version.setRenderProfileVersion(properties.getProfileVersion());
        version.setContentHash(draft.getContentHash());
        version.setManifestHash(DocumentStorage.sha256(draft.getManifestJson()));
        version.setValidationReportJson(report);
        version.setPublishedBy(actor);
        version.setPublishedAt(now());
        version = versions.saveAndFlush(version);

        for (var entry : assets.manifest(draft.getManifestJson()).entrySet()) {
            var ref = new InvoiceTemplateVersionAssetEntity();
            ref.setTemplateVersionId(version.getId());
            ref.setAssetId(entry.getValue().assetId());
            ref.setLogicalName(entry.getKey());
            ref.setExpectedSha256(entry.getValue().sha256());
            ref.setUsage("LAYOUT_ASSET");
            versionAssets.save(ref);
        }
        assets.pin(draft.getManifestJson(), "VERSION", version.getId());
        return version;
    }

    private InvoiceTemplateVersionEntity copyPublishedVersion(InvoiceTemplateEntity target,
                                                               InvoiceTemplateVersionEntity source,
                                                               Long actor) {
        var version = new InvoiceTemplateVersionEntity();
        version.setTemplateId(target.getId());
        version.setVersionNo(1);
        version.setCapabilitiesJson(source.getCapabilitiesJson());
        version.setSourceContent(source.getSourceContent());
        version.setManifestJson(source.getManifestJson());
        version.setSchemaVersion(source.getSchemaVersion());
        version.setFieldCatalogVersion(source.getFieldCatalogVersion());
        version.setCompilerVersion(source.getCompilerVersion());
        version.setRenderProfileVersion(source.getRenderProfileVersion());
        version.setContentHash(source.getContentHash());
        version.setManifestHash(source.getManifestHash());
        version.setValidationReportJson(source.getValidationReportJson());
        version.setPublishedBy(actor);
        version.setPublishedAt(now());
        version = versions.saveAndFlush(version);

        for (var entry : assets.manifest(source.getManifestJson()).entrySet()) {
            var ref = new InvoiceTemplateVersionAssetEntity();
            ref.setTemplateVersionId(version.getId());
            ref.setAssetId(entry.getValue().assetId());
            ref.setLogicalName(entry.getKey());
            ref.setExpectedSha256(entry.getValue().sha256());
            ref.setUsage("LAYOUT_ASSET");
            versionAssets.save(ref);
        }
        assets.pin(version.getManifestJson(), "VERSION", version.getId());
        return version;
    }

    public JsonNode tree(String value) { try { return mapper.readTree(value); } catch (Exception ex) { throw TemplateException.invalid("INVALID_JSON", "JSON không hợp lệ"); } }
    public static String hash(String source, String manifest) { return DocumentStorage.sha256(source + "\n" + manifest); }
    private static LocalDateTime now() { return LocalDateTime.now(ZoneOffset.UTC); }
    private static void checkRevision(InvoiceTemplateDraftEntity draft, long expected) { if (!Objects.equals(draft.getLockVersion(), expected)) throw TemplateException.conflict("Bản nháp đã được người khác cập nhật; vui lòng tải lại"); }
    private static void validateName(String name, int category) { if (name == null || name.isBlank() || name.length() > 255 || !Set.of(1, 2).contains(category)) throw TemplateException.invalid("INVALID_TEMPLATE", "Tên mẫu hoặc loại hóa đơn không hợp lệ"); }
    private static String commandKey(TemplateAccess access, String action, String key) {
        if (key == null || key.isBlank() || key.length() > 128) throw TemplateException.invalid("IDEMPOTENCY_KEY_REQUIRED", "Thiếu Idempotency-Key hợp lệ");
        return DocumentStorage.sha256(access.companyId() + ":" + access.actorId() + ":" + action + ":" + key);
    }
    private static void verifyRequest(InvoiceTemplateAuditEntity event, String hash) { if (!Objects.equals(event.getRequestHash(), hash)) throw TemplateException.conflict("Idempotency-Key đã được dùng với nội dung khác"); }
    private void event(InvoiceTemplateEntity template, TemplateAccess access, String action, String key, String hash, Long resultId, String details) {
        var event = new InvoiceTemplateAuditEntity();
        event.setCompanyId(template.getCompanyId());
        event.setActorId(access.actorId());
        event.setTemplateId(template.getId());
        event.setAction(action);
        event.setRequestKey(key);
        event.setRequestHash(hash);
        event.setResultId(resultId);
        event.setDetailsJson(details);
        event.setCreatedAt(now());
        audit.save(event);
    }
}
