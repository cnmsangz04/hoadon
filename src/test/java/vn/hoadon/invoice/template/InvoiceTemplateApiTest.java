package vn.hoadon.invoice.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.invoice.template.render.*;

import java.nio.file.*;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "invoice.template.poll-delay-ms=3600000",
        "spring.datasource.url=jdbc:h2:mem:invoice_template_api;MODE=MSSQLServer;DB_CLOSE_DELAY=-1",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration/h2",
        "logging.level.org.hibernate.SQL=ERROR"
})
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class InvoiceTemplateApiTest {
    private static final Path STORAGE = temporaryStorage();
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired javax.sql.DataSource dataSource;
    @Autowired InvoiceRenderWorker worker;
    @Autowired vn.hoadon.invoice.template.service.InvoiceTemplateService templates;
    @Autowired vn.hoadon.invoice.template.service.InvoiceFormTemplateService formTemplates;
    @Autowired vn.hoadon.repositories.FormInvoiceRepository forms;
    @Autowired vn.hoadon.invoice.template.persistence.InvoiceRenderJobRepository jobs;
    @Autowired vn.hoadon.invoice.template.persistence.InvoiceTemplateFieldRepository templateFields;
    @Autowired vn.hoadon.invoice.template.catalog.InvoiceFieldCatalog catalog;
    @MockitoBean InvoicePdfRenderer renderer;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) { registry.add("invoice.template.storage-root", STORAGE::toString); }

    @BeforeEach
    void renderer() {
        TestInvoiceFieldCatalog.seed(dataSource, mapper);
        when(renderer.render(anyString(), anyMap())).thenReturn(new InvoicePdfRenderer.Result("%PDF-1.7\napi-fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8), 1));
    }

    @Test
    void administratorCanManageXmlAndInvoiceFields() throws Exception {
        var admin=principal(null);
        mvc.perform(get("/v1/administrator/invoice-template-fields").param("group","invoice").with(authentication(admin)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.items[?(@.key == 'invoice.codeCqt')].sourceType").value(org.hamcrest.Matchers.hasItem("CONTEXT")));
        Map<String,Object> body=new LinkedHashMap<>();
        body.put("key","buyer.AdminNote");body.put("label","Ghi chú người mua");body.put("group","buyer");body.put("dataset","document");
        body.put("sourceType","XML");body.put("sourcePath","DLHDon/NDHDon/NMua/AdminNote");body.put("valueType","STRING");body.put("defaultFormatter","plainText");body.put("active",true);
        String created=mvc.perform(post("/v1/administrator/invoice-template-fields").with(authentication(admin)).contentType("application/json").content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").isNumber()).andReturn().getResponse().getContentAsString();
        var field=mapper.readTree(created);
        body.put("label","Ghi chú người mua cập nhật");body.put("expectedLockVersion",field.path("lockVersion").asLong());
        mvc.perform(put("/v1/administrator/invoice-template-fields/{id}",field.path("id").asLong()).with(authentication(admin)).contentType("application/json").content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.label").value("Ghi chú người mua cập nhật"));
        mvc.perform(get("/v1/invoice-templates/catalog").with(authentication(principal(991L))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.fields[?(@.key == 'buyer.AdminNote')].label").value(org.hamcrest.Matchers.hasItem("Ghi chú người mua cập nhật")));
    }

    @Test
    void catalogEditsPreserveExistingDraftUntilExplicitUpgrade() throws Exception {
        var access = new vn.hoadon.invoice.template.service.TemplateAccess(991L, 999L, false);
        var design = templates.create("Mẫu giữ danh mục", 1, null, false, UUID.randomUUID().toString(), access);
        long id=design.template().getId();
        String originalCode=design.draft().getFieldCatalogVersion();
        var layout=mapper.readTree(design.draft().getSourceContent());
        String before=templates.designerPreview(id,layout,1,access);
        var original=catalog.snapshot(originalCode).requireField("seller.Ten");
        Map<String,Object> body=new LinkedHashMap<>(mapper.convertValue(original,Map.class));
        body.put("expectedLockVersion",original.lockVersion());
        body.put("sourcePath","DLHDon/NDHDon/NMua/Ten");
        var request=put("/v1/administrator/invoice-template-fields/{id}",original.id())
                .with(authentication(principal(null))).contentType("application/json").content(mapper.writeValueAsString(body));
        mvc.perform(request).andExpect(status().isOk());
        String latestCode=catalog.version();
        assertThat(latestCode).isNotEqualTo(originalCode);
        assertThat(catalog.snapshot(originalCode).requireField("seller.Ten").sourcePath()).isEqualTo(original.sourcePath());
        assertThat(templates.designerPreview(id,layout,1,access)).isEqualTo(before);
        mvc.perform(put("/v1/administrator/invoice-template-fields/{id}",original.id()).with(authentication(principal(null)))
                .contentType("application/json").content(mapper.writeValueAsString(body))).andExpect(status().isConflict());
        assertThat(catalog.version()).isEqualTo(latestCode);
        var upgraded=templates.upgradeCatalog(id,design.draft().getLockVersion(),access);
        assertThat(upgraded.getFieldCatalogVersion()).isEqualTo(latestCode);
        assertThat(templates.designerPreview(id,mapper.readTree(upgraded.getSourceContent()),1,access)).isNotEqualTo(before);
    }

    @Test
    void invalidXmlPathRollsBackFieldAndAutomaticCatalogCheckpoint() throws Exception {
        var field=catalog.latest().requireField("seller.Ten");String code=catalog.version();
        Map<String,Object> body=new LinkedHashMap<>(mapper.convertValue(field,Map.class));
        body.put("expectedLockVersion",field.lockVersion());body.put("sourcePath","//Ten[1]");
        mvc.perform(put("/v1/administrator/invoice-template-fields/{id}",field.id()).with(authentication(principal(null)))
                .contentType("application/json").content(mapper.writeValueAsString(body))).andExpect(status().isUnprocessableEntity());
        assertThat(templateFields.findById(field.id()).orElseThrow().getSourcePath()).isEqualTo(field.sourcePath());
        assertThat(catalog.version()).isEqualTo(code);
    }

    @Test
    void liveDesignerPreviewUsesCanonicalCompilerWithoutSavingOrCreatingJobs() throws Exception {
        var owner = principal(991L);
        var access = new vn.hoadon.invoice.template.service.TemplateAccess(991L, 999L, false);
        var design = templates.create("Mẫu xem trực tiếp", 1, null, false, UUID.randomUUID().toString(), access);
        var layout = mapper.readTree(design.draft().getSourceContent());
        var text = (com.fasterxml.jackson.databind.node.ObjectNode) findBlock(layout.path("body"), "block25");
        text.put("text", "Đã sửa <văn bản>");
        long before = jobs.count();
        String body = mapper.writeValueAsString(Map.of("layout", layout, "rows", 3));
        String result = mvc.perform(post("/v1/invoice-templates/{id}/designer-preview", design.template().getId())
                        .with(authentication(owner)).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(result).path("html").asText()).contains("Đã sửa &lt;văn bản&gt;", "data-block-id=\"block25\"", "Hàng hóa, dịch vụ minh họa 3");
        assertThat(templates.details(design.template().getId(), access).draft().getSourceContent()).isEqualTo(design.draft().getSourceContent());
        assertThat(jobs.count()).isEqualTo(before);
        mvc.perform(post("/v1/invoice-templates/{id}/designer-preview", design.template().getId())
                        .with(authentication(principal(992L))).contentType("application/json").content(body)).andExpect(status().isNotFound());
        mvc.perform(post("/v1/invoice-templates/{id}/designer-preview", design.template().getId())
                        .with(authentication(owner)).contentType("application/json").content(mapper.writeValueAsString(Map.of("layout", layout, "rows", 61))))
                .andExpect(status().isBadRequest());
        String fonts = mvc.perform(get("/v1/invoice-templates/designer-fonts").with(authentication(owner)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(mapper.readTree(fonts).path("css").asText()).contains("invoice-body", "invoice-sans", "data:font/ttf;base64,").doesNotContain("https://");
    }

    @Test
    void newSystemFormsOpenDesigner() throws Exception {
        String response = mvc.perform(multipart("/v1/administrator/form-invoices/saveOrUpdate")
                        .param("name", "Mẫu soạn thảo mới").param("category", "1").param("type", "1").param("status", "0")
                        .with(authentication(principal(991L))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.templateId").isNumber())
                .andExpect(jsonPath("$.file").doesNotExist()).andReturn().getResponse().getContentAsString();
        long formId = mapper.readTree(response).path("id").asLong();
        mvc.perform(post("/v1/administrator/invoice-templates/from-form/{id}", formId).header("Idempotency-Key", UUID.randomUUID().toString())
                        .with(authentication(principal(991L))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.draft.sourceContent").isString());
    }

    @Test
    void templatesServeCanonicalArtifactAndRejectCrossCompanyRead() throws Exception {
        var owner = principal(991L);
        mvc.perform(get("/v1/form-invoices/capabilities").with(authentication(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.customTemplates").value(true)).andExpect(jsonPath("$.pdfImport").value(false));
        mvc.perform(get("/v1/invoice-templates/catalog").with(authentication(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.fields.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(90)))
                .andExpect(jsonPath("$.fields[?(@.key == 'common.THDon')].dataset")
                        .value(org.hamcrest.Matchers.hasItem("document")));
        String response = mvc.perform(post("/v1/invoice-templates").with(authentication(owner)).header("Idempotency-Key", UUID.randomUUID().toString())
                .contentType("application/json").content("{\"name\":\"Mẫu API\",\"category\":1,\"companyId\":992,\"scope\":\"SYSTEM\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.template.companyId").value(991)).andExpect(jsonPath("$.template.scope").value("COMPANY"))
                .andReturn().getResponse().getContentAsString();
        var details = mapper.readTree(response);
        long templateId = details.path("template").path("id").asLong();
        long revision = details.path("draft").path("lockVersion").asLong();
        response = mvc.perform(post("/v1/invoice-templates/{id}/preview", templateId).with(authentication(owner)).contentType("application/json")
                        .content("{\"expectedLockVersion\":" + revision + "}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        long jobId = mapper.readTree(response).path("jobId").asLong();
        worker.renderNow(jobId);
        response = mvc.perform(get("/v1/template-render-jobs/{id}", jobId).with(authentication(owner))).andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("SUCCEEDED")).andReturn().getResponse().getContentAsString();
        long artifactId = mapper.readTree(response).path("artifactId").asLong();
        var inline = mvc.perform(get("/v1/template-artifacts/{id}/content", artifactId).with(authentication(owner))).andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf")).andReturn().getResponse();
        var download = mvc.perform(get("/v1/template-artifacts/{id}/content", artifactId).param("disposition", "attachment").with(authentication(owner)))
                .andExpect(status().isOk()).andReturn().getResponse();
        assertThat(inline.getContentAsByteArray()).isEqualTo(download.getContentAsByteArray());
        assertThat(inline.getHeader("ETag")).isEqualTo(download.getHeader("ETag"));
        mvc.perform(get("/v1/template-artifacts/{id}/content", artifactId).with(authentication(principal(992L)))).andExpect(status().isNotFound());
        mvc.perform(get("/v1/invoice-templates/{id}", templateId).with(authentication(principal(992L)))).andExpect(status().isNotFound());
    }

    @Test
    void libraryFiltersBeforePaginationAndExcludesInactiveTemplates() throws Exception {
        for (int i = 0; i < 4; i++) {
            var form = new vn.hoadon.entity.FormInvoiceEntity();
            form.setCompanyId(991L);
            form.setSystem(0);
            form.setCategory(1);
            form.setType(1);
            form.setStatus(i == 3 ? 0 : 1);
            form.setName("Library pagination fixture " + i);
            forms.saveAndFlush(form);
        }
        mvc.perform(get("/v1/form-invoices/templates").param("q", "Library pagination fixture").param("page", "2").param("size", "1").with(authentication(principal(991L))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.total").value(3)).andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.current_page").value(2)).andExpect(jsonPath("$.last_page").value(3));
    }

    @Test
    void existingCustomerLayoutUsesTheSameArtifactForViewAndDownload() throws Exception {
        var form = layoutForm(1);
        var owner = principal(991L);
        var view = mvc.perform(get("/v1/form-invoices/{id}/view", form.getId()).with(authentication(owner)))
                .andExpect(status().isOk()).andReturn().getResponse();
        String artifactId = view.getHeader("X-Artifact-Id");
        assertThat(artifactId).isNotBlank();
        assertThat(view.getContentAsString()).contains("window.invoiceArtifactId=" + artifactId);
        mvc.perform(get("/v1/form-invoices/{id}/download-pdf", form.getId()).param("artifactId", artifactId)
                        .with(authentication(owner)))
                .andExpect(status().isOk()).andExpect(header().string("X-Artifact-Id", artifactId))
                .andExpect(content().bytes("%PDF-1.7\napi-fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        mvc.perform(get("/v1/form-invoices/{id}/download-xml", form.getId()).with(authentication(owner)))
                .andExpect(status().isOk()).andExpect(content().contentType("application/xml"));
        mvc.perform(get("/v1/form-invoices/{id}/view", form.getId()).with(authentication(principal(992L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void publicLayoutAliasesServeOneArtifactWithoutAFeatureFlag() throws Exception {
        var form = layoutForm(0);
        String artifactId = null;
        for (String suffix : List.of("view", "view-pdf-html")) {
            var view = mvc.perform(get("/v1/file/{id}/" + suffix, form.getId()))
                    .andExpect(status().isOk()).andReturn().getResponse();
            if (artifactId == null) artifactId = view.getHeader("X-Artifact-Id");
            assertThat(view.getHeader("X-Artifact-Id")).isEqualTo(artifactId);
        }
        assertThat(artifactId).isNotBlank();
        mvc.perform(get("/v1/file/{id}/download-pdf", form.getId()).param("artifactId", artifactId))
                .andExpect(status().isOk()).andExpect(header().string("X-Artifact-Id", artifactId))
                .andExpect(content().bytes("%PDF-1.7\napi-fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    void sampleXmlDoesNotRequireDesignSourceOrCreatePdfJobs() throws Exception {
        var form = layoutForm(1);
        form.setTemplateVersionId(null);
        forms.saveAndFlush(form);
        long before = jobs.count();
        mvc.perform(get("/v1/form-invoices/{id}/download-xml", form.getId()).with(authentication(principal(991L))))
                .andExpect(status().isOk()).andExpect(content().contentType("application/xml"));
        assertThat(jobs.count()).isEqualTo(before);
    }

    @Test
    void administratorsCanPreviewInactiveSystemTemplatesWithoutMakingThemPublic() throws Exception {
        var form = layoutForm(0);
        form.setStatus(0);
        forms.saveAndFlush(form);
        mvc.perform(get("/v1/administrator/form-invoices/{id}/preview", form.getId()).with(authentication(principal(991L))))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andExpect(header().exists("X-Artifact-Id"))
                .andExpect(content().bytes("%PDF-1.7\napi-fixture".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        mvc.perform(get("/v1/file/{id}/view", form.getId())).andExpect(status().isNotFound());
    }

    private vn.hoadon.entity.FormInvoiceEntity layoutForm(int system) throws Exception {
        var form = new vn.hoadon.entity.FormInvoiceEntity();
        form.setCompanyId(system == 0 ? null : 991L);
        form.setSystem(system);
        form.setCategory(1);
        form.setType(1);
        form.setHaveCode(1);
        form.setFormCode("1");
        form.setSerial("C26TAA");
        form.setStatus(1);
        form.setName("Mẫu hóa đơn hiện có");
        form = forms.saveAndFlush(form);
        var access = new vn.hoadon.invoice.template.service.TemplateAccess(991L, 999L, system == 0);
        var design = formTemplates.editable(form.getId(), UUID.randomUUID().toString(), access);
        var report = templates.validate(design.template().getId(), design.draft().getLockVersion(), access);
        for (var id : mapper.readTree(report.getJobIdsJson())) worker.renderNow(id.asLong());
        templates.validation(report.getId(), access);
        var version = templates.publish(design.template().getId(), design.draft().getLockVersion(), report.getId(), UUID.randomUUID().toString(), access);
        form.setTemplateVersionId(version.getId());
        return forms.saveAndFlush(form);
    }

    private static UsernamePasswordAuthenticationToken principal(Long companyId) {
        var user = new UserEntity();
        user.setId(999L);
        user.setCompanyId(companyId);
        user.setRole(0);
        user.setStatus((byte) 1);
        user.setUsername("invoice-test");
        return new UsernamePasswordAuthenticationToken(user, null, List.of());
    }

    private static Path temporaryStorage() {
        try { return Files.createTempDirectory("invoice-template-api-"); }
        catch (java.io.IOException ex) { throw new IllegalStateException(ex); }
    }

    private static com.fasterxml.jackson.databind.JsonNode findBlock(com.fasterxml.jackson.databind.JsonNode node, String id) {
        if (id.equals(node.path("id").asText())) return node;
        for (var child : node.path("children")) {
            var found = findBlock(child, id);
            if (found != null) return found;
        }
        return null;
    }

    @AfterAll
    static void cleanup() throws Exception { try (var files = Files.walk(STORAGE)) { for (Path path : files.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path); } }
}
