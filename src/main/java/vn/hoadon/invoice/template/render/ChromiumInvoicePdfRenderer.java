package vn.hoadon.invoice.template.render;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Media;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import vn.hoadon.invoice.template.config.InvoiceTemplateProperties;
import vn.hoadon.invoice.template.service.TemplateException;

import java.util.*;

@Component
public class ChromiumInvoicePdfRenderer implements InvoicePdfRenderer {
    private final InvoiceTemplateProperties properties;
    private final String fontCss;
    private final Map<String, byte[]> fontResources;

    public ChromiumInvoicePdfRenderer(InvoiceTemplateProperties properties) {
        this.properties = properties;
        FontBundle fonts = bundledFonts();
        this.fontCss = fonts.css();
        this.fontResources = fonts.resources();
    }

    @Override
    public Result render(String html, Map<Long, Resource> resources) {
        if (html.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > properties.getMaxInputBytes()) throw TemplateException.invalid("RENDER_INPUT_LIMIT", "Nội dung bản in vượt giới hạn");
        Document document = Jsoup.parse(html);
        if (document.body().text().isBlank() && document.select("img,svg").isEmpty()) throw TemplateException.invalid("EMPTY_DOCUMENT", "Mẫu không tạo được nội dung bản in");
        document.select("script,iframe,frame,frameset,object,embed,base,form,input,button,meta[http-equiv=refresh]").remove();
        document.getAllElements().forEach(element -> {
            new ArrayList<>(element.attributes().asList()).stream().filter(a -> a.getKey().toLowerCase(Locale.ROOT).startsWith("on")).forEach(a -> element.removeAttr(a.getKey()));
        });
        document.select("a").forEach(a -> a.removeAttr("href"));
        document.select("img[src],link[href],image[href],image[xlink:href],source[src]").forEach(element -> {
            String value = element.hasAttr("src") ? element.attr("src") : element.hasAttr("href") ? element.attr("href") : element.attr("xlink:href");
            if (value.startsWith("data:") || value.startsWith("#")) return;
            if (value.matches("https://invoice-assets\\.invalid/[0-9]+") && resources.containsKey(Long.valueOf(value.substring(value.lastIndexOf('/') + 1)))) return;
            throw TemplateException.invalid("ASSET_LOAD_FAILED", "Không tải đủ ảnh hoặc font của mẫu");
        });
        document.head().prependElement("meta").attr("charset", "UTF-8");
        document.head().prependElement("style").text(fontCss + "body{font-family:'invoice-body'}");
        document.head().appendElement("style").text("html{-webkit-print-color-adjust:exact;print-color-adjust:exact}");
        List<String> resourceErrors = new ArrayList<>();
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true).setTimeout(properties.getTimeoutSeconds() * 1000.0));
            try (BrowserContext context = browser.newContext(new Browser.NewContextOptions().setJavaScriptEnabled(false).setServiceWorkers(com.microsoft.playwright.options.ServiceWorkerPolicy.BLOCK).setLocale("vi-VN").setTimezoneId("Asia/Ho_Chi_Minh"))) {
                context.route("**/*", route -> {
                    String url = route.request().url();
                    if (url.startsWith("https://invoice-fonts.invalid/")) {
                        byte[] font = fontResources.get(url.substring("https://invoice-fonts.invalid/".length()));
                        if (font != null) {
                            route.fulfill(new Route.FulfillOptions().setBodyBytes(font).setContentType("font/ttf").setHeaders(Map.of("Access-Control-Allow-Origin", "*")));
                            return;
                        }
                    }
                    if (url.matches("https://invoice-assets\\.invalid/[0-9]+")) {
                        Long id = Long.valueOf(url.substring(url.lastIndexOf('/') + 1));
                        Resource resource = resources.get(id);
                        if (resource != null) {
                            route.fulfill(new Route.FulfillOptions().setBodyBytes(resource.bytes()).setContentType(resource.mimeType()).setHeaders(Map.of("Access-Control-Allow-Origin", "*")));
                            return;
                        }
                    }
                    resourceErrors.add("ASSET_RESOURCE_DENIED");
                    route.abort();
                });
                Page page = context.newPage();
                page.onRequestFailed(request -> resourceErrors.add("ASSET_REQUEST_FAILED"));
                page.onConsoleMessage(message -> {
                    if ("error".equals(message.type())) resourceErrors.add("ASSET_BROWSER_ERROR");
                });
                page.setDefaultTimeout(properties.getTimeoutSeconds() * 1000.0);
                var widthMeta = document.selectFirst("meta[name=invoice-printable-width-mm]");
                if (widthMeta != null) {
                    double width = Double.parseDouble(widthMeta.attr("content"));
                    if (width < 50 || width > 300) throw TemplateException.invalid("LAYOUT_OVERFLOW", "Vùng in không hợp lệ");
                    page.setViewportSize((int) Math.ceil(width * 96 / 25.4), 1123);
                }
                page.emulateMedia(new Page.EmulateMediaOptions().setMedia(Media.PRINT));
                page.setContent(document.outerHtml(), new Page.SetContentOptions().setWaitUntil(com.microsoft.playwright.options.WaitUntilState.DOMCONTENTLOADED));
                if (!resourceErrors.isEmpty()) throw TemplateException.invalid("ASSET_LOAD_FAILED", "Không tải đủ ảnh hoặc font của mẫu");
                long resourceDeadline = System.nanoTime() + properties.getTimeoutSeconds() * 1_000_000_000L;
                // Trang không chạy script; thăm dò từ worker để không phụ thuộc requestAnimationFrame.
                while (!Boolean.TRUE.equals(page.evaluate("() => document.fonts.status === 'loaded' && Array.from(document.images).every(i => i.complete)"))) {
                    if (!resourceErrors.isEmpty()) throw TemplateException.invalid("ASSET_LOAD_FAILED", "Không tải đủ ảnh hoặc font của mẫu");
                    if (System.nanoTime() > resourceDeadline) throw TemplateException.invalid("RENDER_TIMEOUT", "Quá thời gian chờ tài nguyên của mẫu");
                    Thread.sleep(25);
                }
                Boolean imagesReady = (Boolean) page.evaluate("() => Array.from(document.images).every(i => i.naturalWidth > 0)");
                if (!resourceErrors.isEmpty() || !Boolean.TRUE.equals(imagesReady)) throw TemplateException.invalid("ASSET_LOAD_FAILED", "Không tải đủ ảnh hoặc font của mẫu");
                Boolean fontsReady = (Boolean) page.evaluate("() => document.fonts.check(getComputedStyle(document.body).font) && Array.from(document.fonts).every(f => f.status !== 'error')");
                if (!Boolean.TRUE.equals(fontsReady)) throw TemplateException.invalid("FONT_UNAVAILABLE", "Không tải đủ font của mẫu");
                Boolean knownFonts = (Boolean) page.evaluate("""
                        () => {
                          const clean = value => value.replace(/['"]/g, '').trim().toLowerCase();
                          const known = new Set(['serif','sans-serif','monospace', ...Array.from(document.fonts).map(f => clean(f.family))]);
                          return Array.from(document.querySelectorAll('body,body *')).filter(e => Array.from(e.childNodes).some(n => n.nodeType === Node.TEXT_NODE && n.textContent.trim())).every(e => known.has(clean(getComputedStyle(e).fontFamily.split(',')[0])));
                        }
                        """);
                if (!Boolean.TRUE.equals(knownFonts)) throw TemplateException.invalid("FONT_UNAVAILABLE", "Mẫu sử dụng font chưa được đóng gói; hãy chọn font được cấp");
                page.evaluate("() => { const mm = Number(document.querySelector('meta[name=invoice-printable-height-mm]')?.content); const h = mm > 0 ? mm * 96 / 25.4 : 900; document.querySelectorAll('tr').forEach(r => { if (r.getBoundingClientRect().height > h) r.classList.add('oversized'); }); }");
                Boolean clipped = (Boolean) page.evaluate("() => Array.from(document.querySelectorAll('[data-block-id]')).some(e => e.clientWidth > 0 && e.scrollWidth > e.clientWidth + 2)");
                if (Boolean.TRUE.equals(clipped)) throw TemplateException.invalid("LAYOUT_OVERFLOW", "Nội dung vượt chiều rộng khối; cần chỉnh bố cục");
                byte[] pdf = page.pdf(new Page.PdfOptions().setFormat("A4").setPreferCSSPageSize(true).setPrintBackground(true));
                try (PDDocument output = PDDocument.load(pdf)) {
                    int count = output.getNumberOfPages();
                    if (count < 1 || count > properties.getMaxPages()) throw TemplateException.invalid("RENDER_PAGE_LIMIT", "Số trang bản in vượt giới hạn");
                    return new Result(pdf, count);
                }
            } finally { browser.close(); }
        } catch (TemplateException ex) { throw ex; }
        catch (Exception ex) {
            if (ex instanceof InterruptedException) Thread.currentThread().interrupt();
            if (!resourceErrors.isEmpty()) throw TemplateException.invalid("ASSET_LOAD_FAILED", "Không tải đủ ảnh hoặc font của mẫu");
            throw TemplateException.invalid("RENDER_FAILED", "Không tạo được PDF; kiểm tra Chromium, font và nhật ký worker");
        }
    }

    record FontBundle(String css, Map<String, byte[]> resources) { }

    static FontBundle bundledFonts() {
        StringBuilder css = new StringBuilder();
        Map<String, byte[]> resources = new HashMap<>();
        Map<String, List<String>> aliases = Map.of("LiberationSerif", List.of("invoice-body", "Times New Roman", "Times", "Liberation Serif"), "LiberationSans", List.of("invoice-sans", "Arial", "Helvetica", "Liberation Sans"), "LiberationMono", List.of("Courier New", "Courier", "Liberation Mono"));
        for (var entry : aliases.entrySet()) {
            for (String style : List.of("Regular", "Bold", "Italic", "BoldItalic")) {
                try (var stream = new ClassPathResource("invoice-template/fonts/" + entry.getKey() + "-" + style + ".ttf").getInputStream()) {
                    String name = entry.getKey() + "-" + style + ".ttf";
                    resources.put(name, stream.readAllBytes());
                    for (String alias : entry.getValue()) css.append("@font-face{font-family:'").append(alias).append("';src:url('https://invoice-fonts.invalid/").append(name).append("') format('truetype');font-weight:").append(style.contains("Bold") ? "700" : "400").append(";font-style:").append(style.contains("Italic") ? "italic" : "normal").append(";}");
                } catch (Exception ex) { throw new IllegalStateException("Thiếu font trong render profile", ex); }
            }
        }
        return new FontBundle(css.toString(), Map.copyOf(resources));
    }
}
