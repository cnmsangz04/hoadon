package vn.hoadon.invoice.template.render;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.invoice.template.catalog.InvoiceViewModelMapper;
import vn.hoadon.invoice.template.compiler.*;
import vn.hoadon.invoice.template.config.InvoiceTemplateProperties;
import vn.hoadon.invoice.template.persistence.*;
import vn.hoadon.invoice.template.service.*;
import vn.hoadon.invoice.template.storage.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;

@Service
public class TemplateRenderService {
    private final InvoiceRenderJobRepository jobs;
    private final InvoiceRenderArtifactRepository artifacts;
    private final InvoiceAssetService assets;
    private final DocumentStorage storage;
    private final JsonLayoutCompiler compiler;
    private final InvoiceViewModelMapper viewModels;
    private final InvoiceTemplateProperties properties;
    private final ObjectMapper mapper;
    private final InvoiceTemplateRepository templates;
    private final InvoiceTemplateDraftRepository drafts;
    private final InvoiceTemplateVersionRepository versions;
    private final String designerFontCss;

    public TemplateRenderService(InvoiceRenderJobRepository jobs, InvoiceRenderArtifactRepository artifacts, InvoiceAssetService assets,
                                 DocumentStorage storage, JsonLayoutCompiler compiler,
                                 InvoiceViewModelMapper viewModels, InvoiceTemplateProperties properties, ObjectMapper mapper,
                                 InvoiceTemplateRepository templates, InvoiceTemplateDraftRepository drafts, InvoiceTemplateVersionRepository versions) {
        this.jobs = jobs;
        this.artifacts = artifacts;
        this.assets = assets;
        this.storage = storage;
        this.compiler = compiler;
        this.viewModels = viewModels;
        this.properties = properties;
        this.mapper = mapper;
        this.templates = templates;
        this.drafts = drafts;
        this.versions = versions;
        var fonts = ChromiumInvoicePdfRenderer.bundledFonts();
        StringBuilder inlineFonts = new StringBuilder();
        for (String rule : fonts.css().split("}")) {
            if (!rule.startsWith("@font-face{font-family:'invoice-")) continue;
            for (var font : fonts.resources().entrySet()) {
                String url = "https://invoice-fonts.invalid/" + font.getKey();
                if (rule.contains(url)) rule = rule.replace(url, "data:font/ttf;base64," + Base64.getEncoder().encodeToString(font.getValue()));
            }
            inlineFonts.append(rule).append('}');
        }
        this.designerFontCss = inlineFonts.toString();
    }

    @Transactional
    public InvoiceRenderJobEntity enqueue(String scope, Long companyId, String purpose, Long versionId, Long draftId,
                                          Long draftLockVersion, Long snapshotId, String source, String manifest,
                                          String xml, Map<String, String> parameters) {
        if (snapshotId != null && versionId != null) {
            var ready = jobs.findTopBySnapshotIdAndTemplateVersionIdAndPurposeAndStateOrderByIdDesc(snapshotId, versionId, purpose, "SUCCEEDED");
            if (ready.isPresent() && ready.get().getArtifactId() != null && Objects.equals(scope, ready.get().getScope())
                    && Objects.equals(companyId, ready.get().getCompanyId())) return ready.get();
        }
        if (source.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > properties.getMaxInputBytes()
                || xml.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > properties.getMaxInputBytes()) {
            throw TemplateException.invalid("RENDER_INPUT_LIMIT", "Nguồn mẫu hoặc XML vượt giới hạn");
        }
        Long templateId = versionId != null ? versions.findById(versionId).orElseThrow(TemplateException::missing).getTemplateId()
                : drafts.findById(draftId).orElseThrow(TemplateException::missing).getTemplateId();
        templates.lockById(templateId).orElseThrow(TemplateException::missing);
        String html;
        try {
            var layout = mapper.readTree(source);
            html = compileLayout(layout, xml, parameters);
        } catch (TemplateException ex) { throw ex; }
        catch (Exception ex) { throw TemplateException.invalid("INVALID_LAYOUT", "Không biên dịch được bố cục mẫu"); }
        assets.resources(manifest, scope, companyId);
        String parameterJson = assets.json(new TreeMap<>(parameters));
        String inputHash = DocumentStorage.sha256(assets.json(List.of(source, manifest, xml, parameterJson, properties.getProfileVersion(), JsonLayoutCompiler.COMPILER_VERSION, "dataset-resolver/2")));
        String requestKey = DocumentStorage.sha256(assets.json(Arrays.asList(scope, companyId, purpose, versionId, draftId, draftLockVersion, snapshotId, inputHash)));
        var existing = jobs.findByRequestKey(requestKey);
        if (existing.isPresent()) return existing.get();
        InvoiceRenderJobEntity job = new InvoiceRenderJobEntity();
        job.setScope(scope);
        job.setCompanyId(companyId);
        job.setPurpose(purpose);
        job.setTemplateVersionId(versionId);
        job.setDraftId(draftId);
        job.setDraftLockVersion(draftLockVersion);
        job.setSnapshotId(snapshotId);
        job.setRequestKey(requestKey);
        job.setInputHash(inputHash);
        job.setSourceContent(html);
        job.setSourceXml(xml);
        job.setManifestJson(manifest);
        job.setParametersJson(parameterJson);
        job.setProfileVersion(properties.getProfileVersion());
        job.setState("QUEUED");
        job.setAttemptCount(0);
        job.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        job = jobs.saveAndFlush(job);
        assets.pin(manifest, "RENDER_JOB", job.getId());
        return job;
    }

    public InvoiceRenderJobEntity job(Long id, TemplateAccess access) {
        var job = jobs.findById(id).orElseThrow(TemplateException::missing);
        checkAccess(job.getScope(), job.getCompanyId(), job.getDraftId() != null || "DRAFT_PREVIEW".equals(job.getPurpose()), access);
        return job;
    }

    public InvoiceRenderArtifactEntity artifact(Long id, TemplateAccess access) {
        var artifact = artifacts.findById(id).orElseThrow(TemplateException::missing);
        var job = jobs.findById(artifact.getJobId()).orElseThrow(TemplateException::missing);
        checkAccess(artifact.getScope(), artifact.getCompanyId(), job.getDraftId() != null || "DRAFT_PREVIEW".equals(job.getPurpose()), access);
        return artifact;
    }

    private static void checkAccess(String scope, Long companyId, boolean draft, TemplateAccess access) {
        if ("SYSTEM".equals(scope)) {
            if (draft && !access.systemManager()) throw TemplateException.missing();
        } else if (!Objects.equals(companyId, access.companyId())) throw TemplateException.missing();
    }

    public byte[] bytes(InvoiceRenderArtifactEntity artifact) { return storage.read(artifact.getStorageKey(), artifact.getSha256()); }

    public String designerFontCss() { return designerFontCss; }

    public String designerPreview(com.fasterxml.jackson.databind.JsonNode layout, String xml, String manifest, String scope, Long companyId) {
        String html = compileLayout(layout, xml, Map.of("sampleMarker", "SAMPLE", "lookupCode", "SAMPLE0001",
                "lookup_code", "SAMPLE0001", "code_cqt", "MCCQT-MINH-HOA"));
        for (var entry : assets.resources(manifest, scope, companyId).entrySet()) {
            var resource = entry.getValue();
            String url = "https://invoice-assets.invalid/" + entry.getKey();
            String data = "data:" + resource.mimeType() + ";base64," + Base64.getEncoder().encodeToString(resource.bytes());
            html = html.replace("\"" + url + "\"", "\"" + data + "\"").replace("'" + url + "'", "'" + data + "'");
        }
        return html;
    }

    private String compileLayout(com.fasterxml.jackson.databind.JsonNode layout, String xml, Map<String, String> parameters) {
        String html = compiler.compile(layout, viewModels.map(xml, parameters, compiler.catalogSnapshot(layout)));
        return html.replace("</head>", "<style>" + customFontCss(layout) + "</style></head>");
    }

    public ResponseEntity<byte[]> response(InvoiceRenderArtifactEntity artifact, boolean attachment, String name) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).eTag("\"" + artifact.getSha256() + "\"")
                .cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION, (attachment ? ContentDisposition.attachment() : ContentDisposition.inline()).filename(name).build().toString())
                .header("X-Artifact-Id", artifact.getId().toString()).header("X-Content-Type-Options", "nosniff").body(bytes(artifact));
    }

    @Transactional
    public InvoiceRenderJobEntity retry(Long id, TemplateAccess access) {
        var job = jobs.lockById(id).orElseThrow(TemplateException::missing);
        checkAccess(job.getScope(), job.getCompanyId(), job.getDraftId() != null || "DRAFT_PREVIEW".equals(job.getPurpose()), access);
        if ("FAILED".equals(job.getState()) && job.getAttemptCount() < 3) {
            job.setState("QUEUED");
            job.setErrorCode(null);
            job.setErrorMessage(null);
            job.setCompletedAt(null);
        }
        return job;
    }

    private String customFontCss(com.fasterxml.jackson.databind.JsonNode layout) {
        StringBuilder css = new StringBuilder();
        layout.path("fontFamilies").fields().forEachRemaining(family -> family.getValue().fields().forEachRemaining(face -> {
            Long id = layout.path("assets").path(face.getValue().asText()).path("assetId").asLong();
            css.append("@font-face{font-family:'invoice-").append(family.getKey()).append("';src:url('https://invoice-assets.invalid/").append(id).append("');font-weight:").append(face.getKey().toLowerCase(Locale.ROOT).contains("bold") ? "700" : "400").append(";font-style:").append(face.getKey().toLowerCase(Locale.ROOT).contains("italic") ? "italic" : "normal").append(";}");
        }));
        return css.toString();
    }
}
