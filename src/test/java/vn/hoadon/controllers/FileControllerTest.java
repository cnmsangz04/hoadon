package vn.hoadon.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.invoice.template.api.TemplateExceptionHandler;
import vn.hoadon.invoice.template.persistence.InvoiceRenderArtifactEntity;
import vn.hoadon.invoice.template.service.InvoiceDocumentBridge;
import vn.hoadon.invoice.template.service.TemplateAccess;
import vn.hoadon.services.FormInvoiceService;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FileControllerTest {
    private final FormInvoiceService forms = mock(FormInvoiceService.class);
    private final InvoiceDocumentBridge documents = mock(InvoiceDocumentBridge.class);
    private final TemplateAccess access = new TemplateAccess(null, null, false);
    private final InvoiceRenderArtifactEntity artifact = new InvoiceRenderArtifactEntity();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        var form = new FormInvoiceEntity();
        form.setId(10L);
        form.setSystem(0);
        form.setStatus(1);
        when(forms.findById(10L)).thenReturn(Optional.of(form));
        mvc = MockMvcBuilders.standaloneSetup(new FileController(forms, documents))
                .setControllerAdvice(new TemplateExceptionHandler()).build();
    }

    @Test
    void allViewAliasesUseTheSameDocumentViewer() throws Exception {
        when(documents.sample(10L, true, access)).thenReturn(artifact);
        when(documents.viewer(artifact, "/v1/file/10/download-pdf"))
                .thenReturn(ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body("<iframe title='PDF'></iframe>"));
        for (String path : new String[]{"/v1/file/10/view", "/v1/file/file/10/view", "/v1/file/10/view-pdf-html"}) {
            mvc.perform(get(path)).andExpect(status().isOk()).andExpect(content().string("<iframe title='PDF'></iframe>"));
        }
        verify(documents, times(3)).sample(10L, true, access);
        verify(documents, times(3)).viewer(artifact, "/v1/file/10/download-pdf");
    }

    @Test
    void downloadReadsTheRequestedArtifact() throws Exception {
        byte[] pdf = "%PDF-1.7\nimmutable".getBytes(StandardCharsets.UTF_8);
        when(documents.sampleArtifact(10L, true, access, 42L)).thenReturn(artifact);
        when(documents.pdf(artifact, true, "invoice-10.pdf"))
                .thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(pdf));
        mvc.perform(get("/v1/file/10/download-pdf").param("artifactId", "42"))
                .andExpect(status().isOk()).andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdf));
        verify(documents).sampleArtifact(10L, true, access, 42L);
    }

    @Test
    void xmlDownloadUsesTheSharedSampleSource() throws Exception {
        when(documents.sampleXml(10L, true, access)).thenReturn("<HDon/>");
        mvc.perform(get("/v1/file/10/download-xml")).andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_XML)).andExpect(content().string("<HDon/>"));
    }

    @Test
    void companyTemplatesCannotUsePublicRoutes() throws Exception {
        var form = new FormInvoiceEntity();
        form.setSystem(1);
        form.setStatus(1);
        when(forms.findById(10L)).thenReturn(Optional.of(form));
        mvc.perform(get("/v1/file/10/view")).andExpect(status().isNotFound());
        mvc.perform(get("/v1/file/10/download-pdf")).andExpect(status().isNotFound());
        mvc.perform(get("/v1/file/10/download-xml")).andExpect(status().isNotFound());
        verifyNoInteractions(documents);
    }

    @Test
    void inactiveTemplatesCannotUsePublicRoutes() throws Exception {
        var form = new FormInvoiceEntity();
        form.setSystem(0);
        form.setStatus(0);
        when(forms.findById(10L)).thenReturn(Optional.of(form));
        mvc.perform(get("/v1/file/10/view")).andExpect(status().isNotFound());
        verifyNoInteractions(documents);
    }

    @Test
    void missingTemplatesReturnNotFound() throws Exception {
        when(forms.findById(10L)).thenReturn(Optional.empty());
        mvc.perform(get("/v1/file/10/view")).andExpect(status().isNotFound());
        verifyNoInteractions(documents);
    }
}
