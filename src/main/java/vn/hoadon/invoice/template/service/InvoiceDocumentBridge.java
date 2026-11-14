package vn.hoadon.invoice.template.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import vn.hoadon.invoice.template.config.InvoiceTemplateProperties;
import vn.hoadon.invoice.template.persistence.*;
import vn.hoadon.invoice.template.render.*;

import java.util.*;

@Service
public class InvoiceDocumentBridge {
    private final InvoiceTemplateProperties properties;
    private final InvoiceDocumentService documents;
    private final TemplateRenderService renders;
    private final InvoiceRenderJobRepository jobs;
    private final InvoiceRenderArtifactRepository artifacts;
    private final InvoiceDocumentSnapshotRepository snapshots;
    private final InvoiceRenderWorker worker;
    private final vn.hoadon.repositories.FormInvoiceRepository forms;
    private final com.fasterxml.jackson.databind.ObjectMapper mapper;

    public InvoiceDocumentBridge(InvoiceTemplateProperties properties, InvoiceDocumentService documents, TemplateRenderService renders,
                                  InvoiceRenderJobRepository jobs, InvoiceRenderArtifactRepository artifacts,
                                  InvoiceDocumentSnapshotRepository snapshots, InvoiceRenderWorker worker,
                                  vn.hoadon.repositories.FormInvoiceRepository forms, com.fasterxml.jackson.databind.ObjectMapper mapper) {
        this.properties = properties;
        this.documents = documents;
        this.renders = renders;
        this.jobs = jobs;
        this.artifacts = artifacts;
        this.snapshots = snapshots;
        this.worker = worker;
        this.forms = forms;
        this.mapper = mapper;
    }

    public InvoiceRenderArtifactEntity invoice(Long invoiceId, Long artifactId) {
        if (artifactId != null) {
            var artifact = artifacts.findById(artifactId).orElseThrow(TemplateException::missing);
            if (artifact.getSnapshotId() == null) throw TemplateException.missing();
            var snapshot = snapshots.findById(artifact.getSnapshotId()).orElseThrow(TemplateException::missing);
            if (!Objects.equals(snapshot.getInvoiceId(), invoiceId)) throw TemplateException.missing();
            return artifact;
        }
        return ready(documents.invoice(invoiceId));
    }

    public InvoiceRenderArtifactEntity sample(Long formId, boolean publicSample, TemplateAccess access) {
        return ready(documents.sample(formId, publicSample, access));
    }

    public String sampleXml(Long formId, boolean publicSample, TemplateAccess access) {
        return documents.sampleXml(formId, publicSample, access);
    }

    public InvoiceRenderArtifactEntity sampleArtifact(Long formId, boolean publicSample, TemplateAccess access, Long artifactId) {
        if (artifactId == null) return sample(formId, publicSample, access);
        var artifact = renders.artifact(artifactId, access);
        var job = jobs.findById(artifact.getJobId()).orElseThrow(TemplateException::missing);
        if (!"TEMPLATE_SAMPLE".equals(job.getPurpose()) || !Objects.equals(publicSample ? "SYSTEM" : "COMPANY", artifact.getScope())) throw TemplateException.missing();
        var form = forms.findById(formId).orElseThrow(TemplateException::missing);
        if (publicSample ? !Objects.equals(form.getSystem(), 0) || !Objects.equals(form.getStatus(), 1)
                : !Objects.equals(form.getSystem(), 1) || !Objects.equals(form.getCompanyId(), access.companyId())) throw TemplateException.missing();
        try {
            if (!formId.toString().equals(mapper.readTree(job.getParametersJson()).path("formId").asText())) throw TemplateException.missing();
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException(ex); }
        return artifact;
    }

    private InvoiceRenderArtifactEntity ready(InvoiceRenderJobEntity job) {
        if ("QUEUED".equals(job.getState())) {
            worker.renderNow(job.getId());
        }
        long deadline = System.nanoTime() + properties.getTimeoutSeconds() * 1_000_000_000L;
        do {
            job = jobs.findById(job.getId()).orElseThrow(TemplateException::missing);
            if ("SUCCEEDED".equals(job.getState())) return artifacts.findById(job.getArtifactId()).orElseThrow(TemplateException::missing);
            if ("FAILED".equals(job.getState())) throw TemplateException.invalid(job.getErrorCode(), job.getErrorMessage());
            try { Thread.sleep(100); }
            catch (InterruptedException ex) { Thread.currentThread().interrupt(); throw TemplateException.invalid("RENDER_INTERRUPTED", "Yêu cầu tạo bản in đã bị ngắt"); }
        } while (System.nanoTime() < deadline);
        throw new TemplateException("DOCUMENT_RENDER_PENDING", "Bản in đang được tạo; vui lòng thử lại", HttpStatus.SERVICE_UNAVAILABLE);
    }

    public ResponseEntity<byte[]> pdf(InvoiceRenderArtifactEntity artifact, boolean attachment, String name) { return renders.response(artifact, attachment, name); }
    public byte[] bytes(InvoiceRenderArtifactEntity artifact) { return renders.bytes(artifact); }

    public ResponseEntity<String> viewer(InvoiceRenderArtifactEntity artifact, String downloadUrl) {
        String encoded = Base64.getEncoder().encodeToString(bytes(artifact));
        String html = """
                <!doctype html><html><head><meta charset="UTF-8"><style>
                html,body{height:100%;min-height:720px;margin:0;font:14px sans-serif}
                nav{height:40px;display:flex;gap:16px;align-items:center;padding:0 12px;background:#f5f5f5}
                iframe{border:0;width:100%;height:calc(100vh - 40px);min-height:680px}
                </style></head><body><nav><a id="download" download="invoice.pdf">Tải PDF</a><button onclick="printInvoice()">In hóa đơn</button></nav>
                <iframe id="pdf" title="Hóa đơn"></iframe><script>
                const raw=atob('__PDF__');const bytes=new Uint8Array(raw.length);
                for(let i=0;i<raw.length;i++)bytes[i]=raw.charCodeAt(i);
                const url=URL.createObjectURL(new Blob([bytes],{type:'application/pdf'}));
                document.getElementById('pdf').src=url;document.getElementById('download').href=url;
                window.invoiceArtifactId=__ID__;
                window.printInvoice=()=>{try{const w=document.getElementById('pdf').contentWindow;w.focus();w.print();}catch(e){window.open(url,'_blank');}};
                window.addEventListener('beforeunload',()=>URL.revokeObjectURL(url));
                window.parent.postMessage({type:'invoice-artifact',artifactId:__ID__,documentKey:__KEY__},'*');
                </script></body></html>
                """.replace("__PDF__", encoded).replace("__ID__", artifact.getId().toString()).replace("__KEY__", scriptString(downloadUrl));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.TEXT_HTML).header("X-Artifact-Id", artifact.getId().toString()).body(html);
    }

    private String scriptString(String value) {
        try { return mapper.writeValueAsString(value).replace("<", "\\u003c").replace(">", "\\u003e").replace("&", "\\u0026"); }
        catch (com.fasterxml.jackson.core.JsonProcessingException ex) { throw new IllegalStateException(ex); }
    }
}
