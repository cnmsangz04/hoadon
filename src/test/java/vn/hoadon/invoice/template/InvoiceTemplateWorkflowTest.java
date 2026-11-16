package vn.hoadon.invoice.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.invoice.template.catalog.*;
import vn.hoadon.invoice.template.compiler.*;
import vn.hoadon.invoice.template.config.InvoiceTemplateProperties;
import vn.hoadon.invoice.template.persistence.*;
import vn.hoadon.invoice.template.render.*;
import vn.hoadon.invoice.template.service.*;
import vn.hoadon.invoice.template.storage.*;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest(showSql = false)
@Import({InvoiceTemplateWorkflowTest.Config.class, InvoiceTemplateService.class, TemplateRenderService.class,
        InvoiceAssetService.class, DocumentStorage.class, JsonLayoutCompiler.class,
        InvoiceFieldCatalog.class, InvoiceViewModelMapper.class, InvoiceFixtureFactory.class, InvoiceFormTemplateService.class, InvoiceTemplateBindingService.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InvoiceTemplateWorkflowTest {
    private static final TemplateAccess OWNER = new TemplateAccess(101L, 1L, false);
    private static final TemplateAccess OTHER = new TemplateAccess(202L, 2L, false);
    private static Path storageRoot;

    @TestConfiguration
    static class Config {
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
        @Bean InvoiceTemplateProperties properties() throws Exception {
            storageRoot = Files.createTempDirectory("invoice-template-test-");
            var properties = new InvoiceTemplateProperties();
            properties.setStorageRoot(storageRoot.toString());
            return properties;
        }
        @Bean InvoicePdfRenderer renderer() { return (html, resources) -> new InvoicePdfRenderer.Result(("%PDF-1.7\n" + html).getBytes(java.nio.charset.StandardCharsets.UTF_8), 1); }
    }

    @Autowired InvoiceTemplateService service;
    @Autowired InvoiceTemplateValidationRunRepository validations;
    @Autowired InvoiceRenderJobRepository jobs;
    @Autowired InvoiceRenderArtifactRepository artifacts;
    @Autowired InvoiceAssetService assets;
    @Autowired DocumentStorage storage;
    @Autowired TemplateRenderService renders;
    @Autowired InvoicePdfRenderer renderer;
    @Autowired InvoiceTemplateProperties properties;
    @Autowired PlatformTransactionManager manager;
    @Autowired ObjectMapper mapper;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired InvoiceFormTemplateService formTemplates;
    @Autowired vn.hoadon.repositories.FormInvoiceRepository forms;
    @Autowired vn.hoadon.repositories.InvoiceRepository invoices;
    @Autowired InvoiceTemplateBindingService bindings;
    @Autowired InvoiceDocumentSnapshotRepository snapshots;
    @Autowired InvoiceTemplateFieldRepository templateFields;

    @BeforeEach
    void seedTemplateFields() {
        TestInvoiceFieldCatalog.seed(dataSource, mapper);
    }

    @Test
    void publishRequiresMatchingRenderProofAndRetriesReturnSameVersion() throws Exception {
        var template = create();
        var report = validateAndRender(template);
        var version = service.publish(template.template().getId(), template.draft().getLockVersion(), report.getId(), "publish-once", OWNER);
        var retry = service.publish(template.template().getId(), template.draft().getLockVersion(), report.getId(), "publish-once", OWNER);
        assertThat(retry.getId()).isEqualTo(version.getId());
        assertThat(service.details(template.template().getId(), OWNER).versions()).hasSize(1);
        assertThatThrownBy(() -> service.publish(template.template().getId(), 999, report.getId(), "publish-once", OWNER)).isInstanceOf(TemplateException.class).hasMessageContaining("nội dung khác");
    }

    @Test
    void staleValidationCannotPublishNewDraft() throws Exception {
        var template = create();
        var report = validateAndRender(template);
        var layout = mapper.readTree(template.draft().getSourceContent());
        ((ObjectNode) layout.path("theme")).put("fontSizePt", 12);
        var saved = service.save(template.template().getId(), template.draft().getLockVersion(), layout, OWNER);
        assertThatThrownBy(() -> service.publish(template.template().getId(), saved.getLockVersion(), report.getId(), "stale-report", OWNER)).isInstanceOf(TemplateException.class).hasMessageContaining("revision");
    }

    @Test
    void concurrentSavesDoNotOverwriteEachOther() throws Exception {
        var template = create();
        Long id = template.template().getId();
        Long revision = template.draft().getLockVersion();
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int size : List.of(11, 12)) results.add(pool.submit(() -> {
                start.await();
                var layout = mapper.readTree(template.draft().getSourceContent());
                ((ObjectNode) layout.path("theme")).put("fontSizePt", size);
                try { service.save(id, revision, layout, OWNER); return true; }
                catch (TemplateException ex) { assertThat(ex.getCode()).isEqualTo("DRAFT_CONFLICT"); return false; }
            }));
            start.countDown();
            assertThat(List.of(results.get(0).get(10, TimeUnit.SECONDS), results.get(1).get(10, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
        } finally { pool.shutdownNow(); }
    }

    @Test
    void tenantCannotReadTemplateOrBorrowAnotherTenantAsset() throws Exception {
        var template = create();
        assertThatThrownBy(() -> service.details(template.template().getId(), OTHER)).isInstanceOf(TemplateException.class);
        var image = new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "png", bytes);
        var asset = assets.upload(bytes.toByteArray(), "IMAGE", OTHER, false);
        var layout = mapper.readTree(template.draft().getSourceContent());
        ((ObjectNode) layout.path("assets")).set("foreign", mapper.createObjectNode().put("assetId", asset.getId()));
        assertThatThrownBy(() -> service.save(template.template().getId(), template.draft().getLockVersion(), layout, OWNER)).isInstanceOf(TemplateException.class);
    }

    @Test
    void previewAndDownloadUsePersistedArtifactAndSameHash() throws Exception {
        var template = create();
        var job = service.preview(template.template().getId(), template.draft().getLockVersion(), 3, OWNER);
        render(job.getId());
        var ready = jobs.findById(job.getId()).orElseThrow();
        var artifact = renders.artifact(ready.getArtifactId(), OWNER);
        var inline = renders.response(artifact, false, "invoice.pdf");
        var download = renders.response(artifact, true, "invoice.pdf");
        assertThat(inline.getBody()).isEqualTo(download.getBody());
        assertThat(inline.getHeaders().getETag()).isEqualTo(download.getHeaders().getETag()).contains(artifact.getSha256());
        assertThat(DocumentStorage.sha256(download.getBody())).isEqualTo(artifact.getSha256());
        assertThat(service.preview(template.template().getId(), template.draft().getLockVersion(), 3, OWNER).getId()).isEqualTo(job.getId());
        assertThatThrownBy(() -> renders.artifact(artifact.getId(), OTHER)).isInstanceOf(TemplateException.class);
    }

    @Test
    void successfulInvoiceSnapshotKeepsItsPdfWhenRuntimeProfileChanges() throws Exception {
        var design = create();
        var report = validateAndRender(design);
        var version = service.publish(design.template().getId(), design.draft().getLockVersion(), report.getId(), "snapshot-artifact", OWNER);
        String xml = new InvoiceFixtureFactory(mapper).sample(1, 3);
        var snapshot = new InvoiceDocumentSnapshotEntity();
        snapshot.setInvoiceId(9001L);
        snapshot.setCompanyId(101L);
        snapshot.setTemplateVersionId(version.getId());
        snapshot.setSourceStage("DRAFT");
        snapshot.setSourceXml(xml);
        snapshot.setSourceHash(DocumentStorage.sha256(xml));
        snapshot.setContextJson("{}");
        snapshot.setFidelity("PINNED_VERSION");
        snapshot.setCreatedAt(java.time.LocalDateTime.now());
        snapshot = snapshots.saveAndFlush(snapshot);
        var job = renders.enqueue("COMPANY", 101L, "INVOICE", version.getId(), null, null, snapshot.getId(), version.getSourceContent(), version.getManifestJson(), xml, Map.of());
        render(job.getId());
        String previous = properties.getProfileVersion();
        try {
            properties.setProfileVersion(previous + "-next");
            var reused = renders.enqueue("COMPANY", 101L, "INVOICE", version.getId(), null, null, snapshot.getId(), version.getSourceContent(), version.getManifestJson(), xml, Map.of());
            assertThat(reused.getId()).isEqualTo(job.getId());
            assertThat(reused.getArtifactId()).isNotNull();
            assertThat(reused.getProfileVersion()).isEqualTo(previous);
        } finally {
            properties.setProfileVersion(previous);
        }
    }

    @Test
    void modifyingDraftKeepsPublishedSourceAndOldPreview() throws Exception {
        var template = create();
        var report = validateAndRender(template);
        var published = service.publish(template.template().getId(), template.draft().getLockVersion(), report.getId(), "immutable-version", OWNER);
        var current = service.details(template.template().getId(), OWNER);
        var layout = mapper.readTree(current.draft().getSourceContent());
        ((ObjectNode) layout.path("theme")).put("fontSizePt", 14);
        service.save(template.template().getId(), current.draft().getLockVersion(), layout, OWNER);
        var read = service.requireVersion(published.getId(), OWNER);
        assertThat(read.getSourceContent()).isEqualTo(published.getSourceContent());
        assertThat(mapper.readTree(read.getSourceContent()).path("theme").path("fontSizePt").asInt()).isEqualTo(11);
    }

    @Test
    void formDesignerCreatesOneCompanyDraftAndRejectsOtherOwners() {
        var form = form(101L, 1);
        form = forms.saveAndFlush(form);
        Long id = form.getId();
        var first = formTemplates.editable(id, "open-designer", OWNER);
        var second = formTemplates.editable(id, "open-designer-again", OWNER);
        assertThat(first.template().getId()).isEqualTo(second.template().getId());
        assertThat(first.draft().getSourceContent()).contains("\"schemaVersion\"");
        assertThat(forms.findById(id).orElseThrow().getTemplateId()).isEqualTo(first.template().getId());
        assertThatThrownBy(() -> formTemplates.editable(id, "wrong-owner", OTHER)).isInstanceOf(TemplateException.class);
        assertThatThrownBy(() -> formTemplates.published(id)).isInstanceOf(TemplateException.class).hasMessageContaining("chưa được công bố");
    }

    @Test
    void changingDefaultPreservesPinnedInvoiceVersionAndFiscalData() throws Exception {
        var first = create();
        var firstReport = validateAndRender(first);
        var oldVersion = service.publish(first.template().getId(), first.draft().getLockVersion(), firstReport.getId(), "first-default", OWNER);
        var form = form(101L, 1);
        form.setTemplateVersionId(oldVersion.getId());
        form = forms.saveAndFlush(form);
        var invoice = historicalInvoice(form.getId());
        bindings.pin(invoice);
        invoice = invoices.saveAndFlush(invoice);
        var design = create();
        var report = validateAndRender(design);
        var version = service.publish(design.template().getId(), design.draft().getLockVersion(), report.getId(), "next-default", OWNER);
        bindings.bindForm(form.getId(), version.getId(), OWNER);
        var historical = invoices.findById(invoice.getId()).orElseThrow();
        assertThat(historical.getTemplateVersionId()).isEqualTo(oldVersion.getId()).isNotEqualTo(version.getId());
        assertThat(historical.getNo()).isEqualTo(128);
        assertThat(historical.getStatus()).isEqualTo((short) 3);
        assertThat(historical.getIssueProfileSnapshotJson()).contains("C26TAA");
        assertThat(forms.findById(form.getId()).orElseThrow().getTemplateVersionId()).isEqualTo(version.getId());
    }

    @Test
    void companyCanUsePublishedSystemVersionWithAnIndependentDesignerDraft() throws Exception {
        var admin = new TemplateAccess(101L, 1L, true);
        var design = service.create("Mẫu hệ thống", 1, null, true, UUID.randomUUID().toString(), admin);
        var run = service.validate(design.template().getId(), design.draft().getLockVersion(), admin);
        for (var id : mapper.readTree(run.getJobIdsJson())) render(id.asLong());
        service.validation(run.getId(), admin);
        var version = service.publish(design.template().getId(), design.draft().getLockVersion(), run.getId(), "system-version", admin);
        var form = form(202L, 1);
        form.setTemplateVersionId(version.getId());
        form = forms.saveAndFlush(form);
        var invoice = historicalInvoice(form.getId());
        invoice.setCompanyId(202);
        bindings.pin(invoice);
        var copy = formTemplates.editable(form.getId(), "customer-designer", OTHER);
        assertThat(invoice.getTemplateVersionId()).isEqualTo(version.getId());
        assertThat(invoice.getIssueProfileSnapshotJson()).contains("C26TAA");
        assertThat(copy.template().getScope()).isEqualTo("COMPANY");
        assertThat(copy.template().getCompanyId()).isEqualTo(202L);
        assertThat(copy.template().getId()).isNotEqualTo(design.template().getId());
        assertThat(copy.draft().getSourceContent()).isEqualTo(version.getSourceContent());
    }

    private static vn.hoadon.entity.FormInvoiceEntity form(Long companyId, int system) {
        var form = new vn.hoadon.entity.FormInvoiceEntity();
        form.setCompanyId(companyId);
        form.setUserId(1L);
        form.setName("Mẫu bố cục");
        form.setSystem(system);
        form.setCategory(1);
        form.setType(1);
        form.setStatus(0);
        form.setFormCode("1");
        form.setSerial("C26TAA");
        form.setHaveCode(1);
        return form;
    }

    private static vn.hoadon.entity.InvoiceEntity historicalInvoice(Long formId) {
        var invoice = new vn.hoadon.entity.InvoiceEntity();
        invoice.setCompanyId(101);
        invoice.setUserId(1);
        invoice.setFormId(Math.toIntExact(formId));
        invoice.setInvoiceNumberId(1);
        invoice.setName("Hóa đơn lịch sử");
        invoice.setNo(128);
        invoice.setStatusConvert((short) 0);
        invoice.setCurrency("VND");
        invoice.setExchangeRate(1d);
        invoice.setSendMailDirectly((short) 0);
        invoice.setDiscount(0d);
        invoice.setVatRate((short) 0);
        invoice.setVatRateOther(0);
        invoice.setVatAmount(0d);
        invoice.setVatAmountDiscount(0d);
        invoice.setTotal(0d);
        invoice.setAmount(0d);
        invoice.setDiscountAmount(0d);
        invoice.setPayment((short) 0);
        invoice.setPaymentType((short) 0);
        invoice.setStatus((short) 3);
        invoice.setInvoiceType((short) 0);
        invoice.setInvoiceTypeAdjust((short) 0);
        invoice.setTypeApi((short) 0);
        return invoice;
    }

    private InvoiceTemplateService.Details create() { return service.create("Mẫu " + UUID.randomUUID(), 1, null, false, UUID.randomUUID().toString(), OWNER); }

    private InvoiceTemplateValidationRunEntity validateAndRender(InvoiceTemplateService.Details template) throws Exception {
        var run = service.validate(template.template().getId(), template.draft().getLockVersion(), OWNER);
        for (var id : mapper.readTree(run.getJobIdsJson())) render(id.asLong());
        var report = service.validation(run.getId(), OWNER);
        assertThat(report.getState()).isEqualTo("PASSED");
        return report;
    }

    private void render(Long id) {
        var worker = new InvoiceRenderWorker(jobs, artifacts, renderer, assets, storage, properties, manager);
        try { worker.renderNow(id); } finally { worker.close(); }
    }

    @AfterAll
    static void cleanStorage() throws Exception {
        if (storageRoot != null) try (var files = Files.walk(storageRoot)) { for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); }
    }
}
