package vn.hoadon.controllers;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.invoice.template.service.InvoiceDocumentBridge;
import vn.hoadon.invoice.template.service.TemplateAccess;
import vn.hoadon.invoice.template.service.TemplateException;
import vn.hoadon.services.FormInvoiceService;

@RestController
@RequestMapping("/v1/file")
public class FileController {
    private final FormInvoiceService formInvoiceService;
    private final InvoiceDocumentBridge documents;

    public FileController(FormInvoiceService formInvoiceService, InvoiceDocumentBridge documents) {
        this.formInvoiceService = formInvoiceService;
        this.documents = documents;
    }

    @GetMapping(value = {"/{id}/view", "/file/{id}/view", "/{id}/view-pdf-html"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> viewHtml(@PathVariable Long id) {
        requirePublicTemplate(id);
        return documents.viewer(documents.sample(id, true, publicAccess()), "/v1/file/" + id + "/download-pdf");
    }

    @GetMapping(value = "/{id}/download-xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> xmlDownload(@PathVariable Long id) {
        requirePublicTemplate(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_XML)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("invoice-" + id + ".xml").build().toString())
                .body(documents.sampleXml(id, true, publicAccess()));
    }

    @GetMapping(value = "/{id}/download-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> pdfDownload(@PathVariable Long id, @RequestParam(required = false) Long artifactId) {
        requirePublicTemplate(id);
        var artifact = documents.sampleArtifact(id, true, publicAccess(), artifactId);
        return documents.pdf(artifact, true, "invoice-" + id + ".pdf");
    }

    private void requirePublicTemplate(Long id) {
        FormInvoiceEntity form = formInvoiceService.findById(id).orElseThrow(TemplateException::missing);
        if (!Integer.valueOf(0).equals(form.getSystem()) || !Integer.valueOf(1).equals(form.getStatus())) {
            throw TemplateException.missing();
        }
    }

    private TemplateAccess publicAccess() {
        return new TemplateAccess(null, null, false);
    }
}
