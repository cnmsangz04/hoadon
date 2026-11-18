package vn.hoadon.invoice.template.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.invoice.template.catalog.InvoiceFixtureFactory;
import vn.hoadon.invoice.template.persistence.InvoiceTemplateDraftEntity;
import vn.hoadon.invoice.template.persistence.InvoiceTemplateEntity;
import vn.hoadon.invoice.template.render.TemplateRenderService;
import vn.hoadon.repositories.FormInvoiceRepository;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.util.UploadPath;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvoiceTemplateThumbnailService {
    private static final Logger log = LoggerFactory.getLogger(InvoiceTemplateThumbnailService.class);
    private final FormInvoiceRepository forms;
    private final CompanyRepository companies;
    private final InvoiceFixtureFactory fixtures;
    private final ObjectMapper mapper;
    private final TemplateRenderService renders;

    public InvoiceTemplateThumbnailService(FormInvoiceRepository forms, CompanyRepository companies,
                                           InvoiceFixtureFactory fixtures, ObjectMapper mapper,
                                           TemplateRenderService renders) {
        this.forms = forms;
        this.companies = companies;
        this.fixtures = fixtures;
        this.mapper = mapper;
        this.renders = renders;
    }

    @Transactional
    public void refresh(InvoiceTemplateEntity template, InvoiceTemplateDraftEntity draft) {
        List<FormInvoiceEntity> linkedForms = forms.findByTemplateId(template.getId());
        if (linkedForms.isEmpty()) return;

        try {
            String xml = fixtures.sample(template.getCategory(), 3);
            com.fasterxml.jackson.databind.JsonNode layout = mapper.readTree(draft.getSourceContent());
            byte[] pdf = renders.renderPdf(layout, xml, draft.getManifestJson(), template.getScope(), template.getCompanyId());
            byte[] png = firstPage(pdf);
            for (FormInvoiceEntity form : linkedForms) update(form, png);
        } catch (Exception ex) {
            log.warn("Không tạo được ảnh xem trước cho template {}: {}", template.getId(), ex.getMessage());
        }
    }

    private void update(FormInvoiceEntity form, byte[] png) throws Exception {
        Long ownerId = form.getCompanyId() != null
                ? form.getCompanyId()
                : companies.findAll().stream().findFirst().map(vn.hoadon.entity.CompanyEntity::getId)
                .orElseThrow(() -> new IllegalStateException("Không có công ty để lưu ảnh mẫu"));
        var directory = UploadPath.resolveCompanyTypeDir(ownerId, "photo");
        var target = directory.resolve("template-" + form.getId() + ".png");
        Files.write(target, png, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        form.setPhoto(UploadPath.publicUrl(ownerId, "photo", target.getFileName().toString()));
        form.setUpdatedAt(LocalDateTime.now());
        forms.save(form);
    }

    private static byte[] firstPage(byte[] pdf) {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdf))) {
            BufferedImage source = new PDFRenderer(document).renderImageWithDPI(0, 110, ImageType.RGB);
            int width = Math.min(source.getWidth(), 760);
            int height = Math.max(1, (int) Math.round(source.getHeight() * (width / (double) source.getWidth())));
            BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = thumbnail.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(source, 0, 0, width, height, null);
            graphics.dispose();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "png", output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Không đọc được PDF để tạo ảnh xem trước", ex);
        }
    }
}
