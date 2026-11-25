package vn.hoadon.invoice.template.render;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
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
    private final InvoicePdfRenderer pdfRenderer;
    private final InvoiceTemplateStorageCleanupService storageCleanup;
    private final String designerFontCss;

    public TemplateRenderService(InvoiceRenderJobRepository jobs, InvoiceRenderArtifactRepository artifacts, InvoiceAssetService assets,
                                 DocumentStorage storage, JsonLayoutCompiler compiler,
                                 InvoiceViewModelMapper viewModels, InvoiceTemplateProperties properties, ObjectMapper mapper,
                                 InvoiceTemplateRepository templates, InvoiceTemplateDraftRepository drafts, InvoiceTemplateVersionRepository versions,
                                 InvoicePdfRenderer pdfRenderer, ObjectProvider<InvoiceTemplateStorageCleanupService> storageCleanup) {
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
        this.pdfRenderer = pdfRenderer;
        this.storageCleanup = storageCleanup.getIfAvailable();
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
        manifest = assets.effectiveManifest(manifest, source, scope, companyId);
        assets.resources(manifest, scope, companyId);
        String parameterJson = assets.json(new TreeMap<>(parameters));
        String inputHash = DocumentStorage.sha256(assets.json(List.of(source, manifest, xml, parameterJson, properties.getProfileVersion(), JsonLayoutCompiler.COMPILER_VERSION, "dataset-resolver/2")));
        String requestKey = DocumentStorage.sha256(assets.json(Arrays.asList(scope, companyId, purpose, versionId, draftId, draftLockVersion, snapshotId, inputHash)));
        var existing = jobs.findByRequestKey(requestKey);
        if (existing.isPresent()) return existing.get();
        if (storageCleanup != null && "DRAFT_PREVIEW".equals(purpose) && draftId != null) storageCleanup.deleteDraftPreviewJobs(draftId);
        if (storageCleanup != null && "TEMPLATE_SAMPLE".equals(purpose) && versionId != null) {
            storageCleanup.deleteCompletedTemplateSamples(scope, companyId, versionId);
        }
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

    public String html(InvoiceRenderJobEntity job) {
        String html = job.getSourceContent();
        for (var entry : assets.resources(job.getManifestJson(), job.getScope(), job.getCompanyId()).entrySet()) {
            String url = "https://invoice-assets.invalid/" + entry.getKey();
            String data = "data:" + entry.getValue().mimeType() + ";base64," + Base64.getEncoder().encodeToString(entry.getValue().bytes());
            html = html.replace("\"" + url + "\"", "\"" + data + "\"")
                    .replace("'" + url + "'", "'" + data + "'");
        }
        String[] margins = previewMargins(job, html);
        String printableWidth = metaValue(html, "invoice-printable-width-mm", "210");
        String printableHeight = metaValue(html, "invoice-printable-height-mm", "297");
        html = html.replace("</head>", "<style id=\"invoice-preview-style\">html{background:#fff}body{box-sizing:border-box;width:calc(" +
                printableWidth + "mm + " + margins[1] + " + " + margins[3] + ");min-height:calc(" + printableHeight +
                "mm + " + margins[0] + " + " + margins[2] + ");margin:0;padding:" +
                margins[0] + " " + margins[1] + " " + margins[2] + " " + margins[3] +
                "}</style></head>");
        return html;
    }

    private String metaValue(String html, String name, String fallback) {
        var meta = java.util.regex.Pattern.compile("<meta\\s+name=\\\"" + java.util.regex.Pattern.quote(name) +
                "\\\"\\s+content=\\\"([^\\\"]+)\\\"", java.util.regex.Pattern.CASE_INSENSITIVE).matcher(html);
        return meta.find() ? meta.group(1).trim() : fallback;
    }

    private String[] previewMargins(InvoiceRenderJobEntity job, String html) {
        var meta = java.util.regex.Pattern.compile("<meta\\s+name=\\\"invoice-page-margin-mm\\\"\\s+content=\\\"([^\\\"]+)\\\"", java.util.regex.Pattern.CASE_INSENSITIVE)
                .matcher(html);
        if (meta.find()) {
            String[] values = meta.group(1).trim().split("\\s+");
            if (values.length == 4) return values;
        }
        try {
            if (job.getTemplateVersionId() != null) {
                var version = versions.findById(job.getTemplateVersionId()).orElse(null);
                if (version != null) {
                    var layout = mapper.readTree(version.getSourceContent());
                    var margin = layout.path("page").path("marginMm");
                    return new String[]{
                            margin.path("top").asText("10") + "mm",
                            margin.path("right").asText("10") + "mm",
                            margin.path("bottom").asText("10") + "mm",
                            margin.path("left").asText("10") + "mm"
                    };
                }
            }
        } catch (Exception ignored) { }
        return new String[]{"10mm", "10mm", "10mm", "10mm"};
    }

    public String designerFontCss() { return designerFontCss; }

    public byte[] renderPdf(com.fasterxml.jackson.databind.JsonNode layout, String xml, String manifest, String scope, Long companyId) {
        String html = compileLayout(layout, xml, Map.of("sampleMarker", "SAMPLE", "lookupCode", "SAMPLE0001",
                "lookup_code", "SAMPLE0001", "code_cqt", "MCCQT-MINH-HOA"));
        return pdfRenderer.render(html, assets.resources(manifest, scope, companyId)).bytes();
    }

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
        Map<String, Object> data = new LinkedHashMap<>(viewModels.map(xml, parameters, compiler.catalogSnapshot(layout)));
        if ("SAMPLE".equals(parameters.get("sampleMarker"))) {
            String signer = Objects.toString(data.get("seller.Ten"), "").trim();
            if (signer.isBlank()) signer = "CÔNG TY MINH HỌA";
            String signedAt = Objects.toString(data.get("common.NLap"), "20-01-2026");
            if (signedAt.isBlank()) signedAt = "20-01-2026";
            try {
                signedAt = java.time.LocalDate.parse(signedAt).format(java.time.format.DateTimeFormatter.ofPattern("dd / MM / yyyy"));
            } catch (RuntimeException ignored) { }
            data.put("presentation.signatureValid", "true");
            data.put("presentation.signatureSigner", signer);
            data.put("presentation.signatureSignedAt", signedAt);
            data.put("invoice.DSCKS.NBan.Signature.SignedAt", signedAt);
        }
        String html = compiler.compile(layout, data);
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
