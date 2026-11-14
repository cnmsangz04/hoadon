package vn.hoadon.invoice.template.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.invoice.template.catalog.*;
import vn.hoadon.invoice.template.compiler.JsonLayoutCompiler;
import vn.hoadon.invoice.template.config.InvoiceTemplateProperties;
import vn.hoadon.invoice.template.persistence.*;
import vn.hoadon.invoice.template.render.TemplateRenderService;
import vn.hoadon.invoice.template.storage.*;

import java.time.*;
import java.nio.charset.StandardCharsets;
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

    public InvoiceTemplateService(InvoiceTemplateRepository templates, InvoiceTemplateDraftRepository drafts,
                                   InvoiceTemplateVersionRepository versions, InvoiceTemplateVersionAssetRepository versionAssets,
                                   InvoiceTemplateValidationRunRepository validations, InvoiceTemplateAuditRepository audit,
                                   InvoiceAssetService assets, TemplateRenderService renders, InvoiceFixtureFactory fixtures,
                                   JsonLayoutCompiler compiler, InvoiceFieldCatalog catalog,
                                   InvoiceTemplateProperties properties, ObjectMapper mapper) {
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
    }

    public Page<InvoiceTemplateEntity> list(TemplateAccess access, String scope, String q, Integer category, int page, int size) {
        Specification<InvoiceTemplateEntity> filter = (root, query, cb) -> {
            var owner = "SYSTEM".equals(scope) ? cb.equal(root.get("scope"), "SYSTEM") : cb.and(cb.equal(root.get("scope"), "COMPANY"), cb.equal(root.get("companyId"), access.companyId()));
            var predicate = cb.and(owner, cb.equal(root.get("state"), "ACTIVE"));
            if (category != null) predicate = cb.and(predicate, cb.equal(root.get("category"), category));
            if (q != null && !q.isBlank()) predicate = cb.and(predicate, cb.like(cb.lower(root.get("name")), "%" + q.trim().toLowerCase(Locale.ROOT).replace("%", "\\%").replace("_", "\\_") + "%", '\\'));
            return predicate;
        };
        return templates.findAll(filter, PageRequest.of(Math.max(0, page - 1), Math.clamp(size, 1, 100), Sort.by(Sort.Direction.DESC, "updatedAt", "id")));
    }

    public Details details(Long id, TemplateAccess access) {
        var template = require(id, access, false);
        var draft = "SYSTEM".equals(template.getScope()) && !access.systemManager() ? null : drafts.findByTemplateId(id).orElse(null);
        return new Details(template, draft, versions.findByTemplateIdOrderByVersionNoDesc(id));
    }

    @Transactional
    public Details create(String name, int category, Long sourceVersionId, boolean system, String key, TemplateAccess access) {
        if (system) access.requireSystemManager(); else access.requireCompany();
        validateName(name, category);
        if (system && !access.systemManager()) throw TemplateException.missing();
        String requestKey = commandKey(access, "CREATE", key);
        String requestHash = DocumentStorage.sha256(assets.json(Arrays.asList(name, category, sourceVersionId, system)));
        var previous = audit.findByRequestKey(requestKey);
        if (previous.isPresent()) {
            verifyRequest(previous.get(), requestHash);
            return details(previous.get().getResultId(), access);
        }
        InvoiceTemplateVersionEntity source = sourceVersionId == null ? null : requireVersion(sourceVersionId, access);
        if (source != null) {
            var parent = require(source.getTemplateId(), access, false);
            if (parent.getCategory() != category || !"ACTIVE".equals(parent.getState())) throw TemplateException.invalid("TEMPLATE_INCOMPATIBLE", "Mẫu nguồn không phù hợp hoặc đã ngừng sử dụng");
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
        String sourceContent = source == null ? defaultLayout() : source.getSourceContent();
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
    public InvoiceTemplateDraftEntity save(Long id, long expectedVersion, JsonNode layout, TemplateAccess access) {
        var template = lock(id, access);
        if (!"ACTIVE".equals(template.getState())) throw TemplateException.conflict("Mẫu đã ngừng sử dụng");
        var draft = draft(id);
        checkRevision(draft, expectedVersion);
        if (!draft.getFieldCatalogVersion().equals(layout.path("fieldCatalogVersion").asText())) throw TemplateException.conflict("Cần cập nhật danh mục bằng thao tác Cập nhật danh mục");
        return saveDraft(template, draft, layout, access);
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
        event(template, access, "SAVE_DRAFT", null, null, draft.getId(), "{}");
        return draft;
    }

    @Transactional
    public InvoiceTemplateDraftEntity upgradeCatalog(Long id, long expectedVersion, TemplateAccess access) {
        var template = lock(id, access);
        if (!"ACTIVE".equals(template.getState())) throw TemplateException.conflict("Mẫu đã ngừng sử dụng");
        var draft = draft(id);
        checkRevision(draft, expectedVersion);
        var layout = (com.fasterxml.jackson.databind.node.ObjectNode) tree(draft.getSourceContent());
        layout.put("fieldCatalogVersion", catalog.version());
        draft = saveDraft(template, draft, layout, access);
        event(template, access, "UPGRADE_CATALOG", null, null, draft.getId(), "{}");
        return draft;
    }

    public String designerFontCss() { return renders.designerFontCss(); }

    @Transactional(readOnly = true)
    public String designerPreview(Long id, JsonNode layout, int rows, TemplateAccess access) {
        var template = require(id, access, true);
        if (!"ACTIVE".equals(template.getState())) throw TemplateException.conflict("Mẫu đã ngừng sử dụng");
        if (rows < 0 || rows > 60) throw TemplateException.invalid("INVALID_ROWS", "Số dòng minh họa phải từ 0 đến 60");
        if (assets.json(layout).getBytes(java.nio.charset.StandardCharsets.UTF_8).length > properties.getMaxInputBytes())
            throw TemplateException.invalid("RENDER_INPUT_LIMIT", "Nội dung bố cục vượt giới hạn");
        compiler.validate(layout, false);
        var manifest = layoutManifest(layout, template);
        return renders.designerPreview(layout, fixtures.sample(template.getCategory(), rows), assets.json(manifest), template.getScope(), template.getCompanyId());
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
        List<Long> jobIds = new ArrayList<>();
        for (int count : List.of(0, 1, 60)) jobIds.add(preview(template, draft, count).getId());
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
        if (!"ACTIVE".equals(template.getState())) throw TemplateException.conflict("Mẫu đã ngừng sử dụng");
        var draft = draft(id);
        checkRevision(draft, expectedVersion);
        var run = validation(reportId, access);
        if (!run.getDraftId().equals(draft.getId()) || !"PASSED".equals(run.getState()) || !run.getContentHash().equals(draft.getContentHash()) || !run.getDraftLockVersion().equals(draft.getLockVersion()) || !run.getManifestHash().equals(DocumentStorage.sha256(draft.getManifestJson())) || !run.getProfileVersion().equals(properties.getProfileVersion())) {
            throw TemplateException.conflict("Bản kiểm tra chưa đạt hoặc không khớp revision hiện tại");
        }
        var version = freeze(template, draft, access.actorId(), run.getReportJson());
        draft.setBaseVersionId(version.getId());
        draft.setUpdatedAt(now());
        template.setUpdatedAt(now());
        event(template, access, "PUBLISH", requestKey, requestHash, version.getId(), run.getReportJson());
        return version;
    }

    @Transactional
    public InvoiceTemplateDraftEntity restore(Long id, Long versionId, long expectedVersion, TemplateAccess access) {
        var template = lock(id, access);
        var source = requireVersion(versionId, access);
        if (!source.getTemplateId().equals(id)) throw TemplateException.missing();
        var draft = draft(id);
        checkRevision(draft, expectedVersion);
        draft.setBaseVersionId(versionId);
        draft.setSourceContent(source.getSourceContent());
        draft.setFieldCatalogVersion(source.getFieldCatalogVersion());
        draft.setManifestJson(source.getManifestJson());
        draft.setContentHash(source.getContentHash());
        draft.setUpdatedBy(access.actorId());
        draft.setUpdatedAt(now());
        event(template, access, "RESTORE_DRAFT", null, null, draft.getId(), assets.json(Map.of("versionId", versionId)));
        draft = drafts.saveAndFlush(draft);

        return draft;
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
        return preview(template, draft, Math.clamp(rows, 0, 100));
    }

    private InvoiceRenderJobEntity preview(InvoiceTemplateEntity template, InvoiceTemplateDraftEntity draft, int rows) {
        return renders.enqueue(template.getScope(), template.getCompanyId(), "DRAFT_PREVIEW", null, draft.getId(), draft.getLockVersion(), null,
                draft.getSourceContent(), draft.getManifestJson(), fixtures.sample(template.getCategory(), rows),
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

    public String defaultLayout() {
        try (var stream = new ClassPathResource("invoice-template/default-layout.json").getInputStream()) {
            var layout = (com.fasterxml.jackson.databind.node.ObjectNode) tree(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            layout.put("fieldCatalogVersion", catalog.version());
            return assets.json(layout);
        } catch (Exception ex) {
            throw new IllegalStateException("Không đọc được bố cục mẫu mặc định", ex);
        }
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
