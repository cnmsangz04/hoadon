package vn.hoadon.invoice.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.invoice.template.config.InvoiceTemplateProperties;
import vn.hoadon.invoice.template.persistence.*;
import vn.hoadon.invoice.template.render.*;
import vn.hoadon.invoice.template.service.*;
import vn.hoadon.repositories.FormInvoiceRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvoiceDocumentBridgeTest {
    private final InvoiceDocumentService documents = mock(InvoiceDocumentService.class);
    private final TemplateRenderService renders = mock(TemplateRenderService.class);
    private final InvoiceRenderJobRepository jobs = mock(InvoiceRenderJobRepository.class);
    private final InvoiceRenderArtifactRepository artifacts = mock(InvoiceRenderArtifactRepository.class);
    private final InvoiceDocumentSnapshotRepository snapshots = mock(InvoiceDocumentSnapshotRepository.class);
    private final FormInvoiceRepository forms = mock(FormInvoiceRepository.class);
    private InvoiceDocumentBridge bridge;

    @BeforeEach
    void setUp() {
        bridge = new InvoiceDocumentBridge(new InvoiceTemplateProperties(), documents, renders, jobs, artifacts, snapshots,
                mock(InvoiceRenderWorker.class), forms, new ObjectMapper());
    }

    @Test
    void downloadKeepsViewedSampleEvenWhenFormNowUsesAnotherVersion() {
        var access = new TemplateAccess(101L, 1L, false);
        var artifact = new InvoiceRenderArtifactEntity();
        artifact.setId(20L);
        artifact.setJobId(30L);
        artifact.setScope("COMPANY");
        artifact.setTemplateVersionId(1L);
        var job = new InvoiceRenderJobEntity();
        job.setPurpose("TEMPLATE_SAMPLE");
        job.setParametersJson("{\"formId\":\"10\"}");
        var form = new FormInvoiceEntity();
        form.setId(10L);
        form.setSystem(1);
        form.setCompanyId(101L);
        form.setTemplateVersionId(2L);
        when(renders.artifact(20L, access)).thenReturn(artifact);
        when(jobs.findById(30L)).thenReturn(Optional.of(job));
        when(forms.findById(10L)).thenReturn(Optional.of(form));
        assertThat(bridge.sampleArtifact(10L, false, access, 20L)).isSameAs(artifact);
        verifyNoInteractions(documents);
    }

    @Test
    void artifactCannotBeSubstitutedForAnotherInvoice() {
        var artifact = new InvoiceRenderArtifactEntity();
        artifact.setSnapshotId(10L);
        var snapshot = new InvoiceDocumentSnapshotEntity();
        snapshot.setInvoiceId(100L);
        when(artifacts.findById(1L)).thenReturn(Optional.of(artifact));
        when(snapshots.findById(10L)).thenReturn(Optional.of(snapshot));
        assertThatThrownBy(() -> bridge.invoice(200L, 1L)).isInstanceOf(TemplateException.class);
        verifyNoInteractions(documents);
    }

    @Test
    void viewerCarriesSameBytesAndArtifactIdentityForDownload() {
        var artifact = new InvoiceRenderArtifactEntity();
        artifact.setId(10L);
        when(renders.bytes(artifact)).thenReturn("%PDF-1.7\nfixture".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        var response = bridge.viewer(artifact, "/v1/invoices/LOOKUP001/download-pdf");
        assertThat(response.getBody()).contains("JVBERi0xLjcKZml4dHVyZQ==", "window.invoiceArtifactId=10", "documentKey:\"/v1/invoices/LOOKUP001/download-pdf\"");
        assertThat(response.getHeaders().getFirst("X-Artifact-Id")).isEqualTo("10");
    }
}
