package vn.hoadon.invoice.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import vn.hoadon.invoice.template.catalog.*;
import vn.hoadon.invoice.template.compiler.JsonLayoutCompiler;
import vn.hoadon.invoice.template.config.InvoiceTemplateProperties;
import vn.hoadon.invoice.template.render.ChromiumInvoicePdfRenderer;
import vn.hoadon.invoice.template.service.TemplateException;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@EnabledIfSystemProperty(named = "invoice.render.browser-tests", matches = "true")
class ChromiumInvoicePdfRendererTest {
    private final InvoiceTemplateProperties properties = new InvoiceTemplateProperties();

    @Test
    void newDefaultTemplateKeepsPartiesTotalsAndSignaturesOnOnePage() throws Exception {
        var mapper = new ObjectMapper();
        var catalog = TestInvoiceFieldCatalog.create(mapper);
        var layout = mapper.readTree(getClass().getResourceAsStream("/invoice-template/default-layout.json"));
        var compiler = new JsonLayoutCompiler(catalog);
        compiler.validate(layout, true);
        String xml = new InvoiceFixtureFactory(mapper).sample(1, 3);
        String html = compiler.compile(layout, new InvoiceViewModelMapper(catalog).map(xml,
                Map.of("sampleMarker", "SAMPLE", "lookup_code", "SAMPLE0001", "code_cqt", "MCCQT-MINH-HOA")));
        var output = new ChromiumInvoicePdfRenderer(properties).render(html, Map.of());
        try (PDDocument pdf = PDDocument.load(output.bytes())) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(1);
            assertThat(new PDFTextStripper().getText(pdf)).contains("Đơn vị bán hàng", "Họ tên người mua hàng", "330.000", "NGƯỜI MUA HÀNG", "NGƯỜI BÁN HÀNG", "SAMPLE0001");
            String directory = System.getProperty("invoice.template.preview-dir");
            if (directory != null) {
                var path = java.nio.file.Files.createDirectories(java.nio.file.Path.of(directory));
                java.nio.file.Files.write(path.resolve("mau-hoa-don-moi.pdf"), output.bytes());
                var image = new org.apache.pdfbox.rendering.PDFRenderer(pdf).renderImageWithDPI(0, 120);
                javax.imageio.ImageIO.write(image, "png", path.resolve("mau-hoa-don-moi.png").toFile());
            }
        }
    }

    @Test
    void embedsVietnameseFontsAndHonorsA5CssWithoutNetwork() throws Exception {
        var output = new ChromiumInvoicePdfRenderer(properties).render("<html><head><style>@page{size:A5;margin:10mm}body{font-family:'Times New Roman'}.header{display:grid;grid-template-columns:1fr 1fr}</style></head><body><div class='header'><div>HÓA ĐƠN GIÁ TRỊ GIA TĂNG</div><div>Đặng Nguyễn — ộ ệ ỷ ữ</div></div></body></html>", Map.of());
        try (PDDocument pdf = PDDocument.load(output.bytes())) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(1);
            assertThat(pdf.getPage(0).getMediaBox().getWidth()).isBetween(419f, 421f);
            String text = new PDFTextStripper().getText(pdf);
            assertThat(text).contains("HÓA ĐƠN GIÁ TRỊ GIA TĂNG", "Đặng Nguyễn", "ộ ệ ỷ ữ");
            for (var font : pdf.getPage(0).getResources().getFontNames()) assertThat(pdf.getPage(0).getResources().getFont(font).isEmbedded()).isTrue();
        }
    }

    @Test
    void dynamicTableKeepsLastRowAndTotalsAcrossPages() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var catalog = TestInvoiceFieldCatalog.create(mapper);
        var layout = mapper.readTree(getClass().getResourceAsStream("/invoice-template/default-layout.json"));
        String xml = new InvoiceFixtureFactory(mapper).sample(1, 160);
        String html = new JsonLayoutCompiler(catalog).compile(layout, new InvoiceViewModelMapper(catalog).map(xml, Map.of()));
        var output = new ChromiumInvoicePdfRenderer(properties).render(html, Map.of());
        try (PDDocument pdf = PDDocument.load(output.bytes())) {
            assertThat(pdf.getNumberOfPages()).isGreaterThan(2);
            assertThat(new PDFTextStripper().getText(pdf)).contains("minh họa 160", "17.600.000", "NGƯỜI BÁN HÀNG");
        }
    }

    @Test
    void htmlEntitiesAndVoidTagsRenderWithoutControllerNormalizers() throws Exception {
        String html = "<html><head><meta charset='UTF-8'></head><body>"
                + "<p>H&oacute;a &nbsp;đơn &amp; thuế &lt;XML&gt;</p><br><hr>"
                + "<img src=\"data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' width='10' height='10'><rect width='10' height='10'/></svg>\">"
                + "</body></html>";
        var result = new ChromiumInvoicePdfRenderer(properties).render(html, Map.of());
        try (PDDocument pdf = PDDocument.load(result.bytes())) {
            assertThat(pdf.getNumberOfPages()).isEqualTo(1);
            String text = new PDFTextStripper().getText(pdf).replace('\u00a0', ' ');
            assertThat(text).contains("Hóa", "đơn & thuế <XML>");
        }
    }

    @Test
    void externalAndMissingImagesFailInsteadOfProducingIncompletePdf() {
        var renderer = new ChromiumInvoicePdfRenderer(properties);
        assertThatThrownBy(() -> renderer.render("<html><body><img src='http://127.0.0.1:1/private'></body></html>", Map.of())).isInstanceOf(TemplateException.class).hasMessageContaining("ảnh hoặc font");
        assertThatThrownBy(() -> renderer.render("<html><body><img src='https://invoice-assets.invalid/999'></body></html>", Map.of())).isInstanceOf(TemplateException.class).hasMessageContaining("ảnh hoặc font");
    }

    @Test
    void packagedStylesheetUsesItsMimeTypeAndUnknownFontIsRejected() throws Exception {
        var renderer = new ChromiumInvoicePdfRenderer(properties);
        byte[] css = "@page{size:A5}body{font-family:'Courier New'}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var output = renderer.render("<html><head><link rel='stylesheet' href='https://invoice-assets.invalid/1'></head><body>Tiếng Việt</body></html>", Map.of(1L, new vn.hoadon.invoice.template.render.InvoicePdfRenderer.Resource(css, "text/css")));
        try (PDDocument pdf = PDDocument.load(output.bytes())) {
            assertThat(pdf.getPage(0).getMediaBox().getWidth()).isBetween(419f, 421f);
            assertThat(new PDFTextStripper().getText(pdf)).contains("Tiếng Việt");
        }
        assertThatThrownBy(() -> renderer.render("<html><body><p style=\"font-family:UnavailableFont\">Không được mất font</p></body></html>", Map.of()))
                .isInstanceOfSatisfying(TemplateException.class, ex -> assertThat(ex.getCode()).isEqualTo("FONT_UNAVAILABLE"));
    }
}
