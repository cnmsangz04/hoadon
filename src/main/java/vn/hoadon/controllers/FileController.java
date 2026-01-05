package vn.hoadon.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.CompanyBankEntity;
import vn.hoadon.entity.LegalRepresentativeEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.CompanyBankRepository;
import vn.hoadon.repositories.LegalRepresentativeRepository;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.services.FormInvoiceService;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.openhtmltopdf.extend.FSUriResolver;

@RestController
@RequestMapping("/v1/file")
public class FileController {

    private final FormInvoiceService formInvoiceService;

    @Autowired private CompanyRepository companyRepository;
    @Autowired private CompanyBankRepository companyBankRepository;
    @Autowired private LegalRepresentativeRepository legalRepresentativeRepository;
    @Autowired private UserRepository userRepository;

    @Autowired
    public FileController(FormInvoiceService formInvoiceService) {
        this.formInvoiceService = formInvoiceService;
    }

    @GetMapping(value = "/{id}/view", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> viewHtml(@PathVariable Long id) {
        return renderHtml(id);
    }

    // Expose the same preview under /v1/file/{id}/view for clients preferring the v1 prefix.
    @GetMapping(value = "/file/{id}/view", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> viewHtmlV1(@PathVariable Long id) {
        return renderHtml(id);
    }

    private ResponseEntity<?> renderHtml(Long id) {
        Optional<FormInvoiceEntity> opt = formInvoiceService.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_HTML).body("<html><body>Form không tồn tại</body></html>");
        }
        FormInvoiceEntity e = opt.get();
        String xsltValue = e.getFile();
        if (!StringUtils.hasText(xsltValue)) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_HTML).body("<html><body>Không tìm thấy tệp XSLT của mẫu</body></html>");
        }

        // Determine if 'file' contains an inline XSLT content or a public file path
        boolean looksLikeXslt = looksLikeInlineXslt(xsltValue);
        Path xsltFsPath = looksLikeXslt ? null : toFilesystemPath(xsltValue);
        if (!looksLikeXslt && (xsltFsPath == null || !Files.exists(xsltFsPath))) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_HTML).body("<html><body>Không tồn tại tệp XSLT trên hệ thống</body></html>");
        }

        // Build sample XML using data from form/company/bank/legal representative
        CompanyEntity company = null;
        CompanyBankEntity bank = null;
        LegalRepresentativeEntity rep = null;
        if (e.getCompanyId() != null) {
            company = companyRepository.findById(e.getCompanyId()).orElse(null);
            if (company != null) {
                List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
                if (banks != null && !banks.isEmpty()) bank = banks.get(0);
            }
            rep = legalRepresentativeRepository.findByCompanyId(e.getCompanyId()).orElse(null);
        }
        UserEntity user = null;
        if (e.getUserId() != null) {
            user = userRepository.findById(e.getUserId()).orElse(null);
        }
        String sampleXml = vn.hoadon.util.SampleInvoiceXmlBuilder.build(user, e, company, bank, rep);
        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            try { factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true); } catch (Exception ignored) {}
            try { factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file"); } catch (Exception ignored) {}

            // Compile XSLT from either inline content or a file path
            StreamSource xsltSource = looksLikeXslt
                    ? new StreamSource(new StringReader(xsltValue))
                    : new StreamSource(xsltFsPath.toFile());
            Transformer transformer = factory.newTransformer(xsltSource);

            StreamSource xmlSource = new StreamSource(new StringReader(sampleXml));
            StringWriter outWriter = new StringWriter();
            StreamResult result = new StreamResult(outWriter);
            transformer.transform(xmlSource, result);
            String html = outWriter.toString();

            // Decode any HTML named entities (e.g., &oacute;, &nbsp;) to Unicode to keep XHTML well-formed for downstream parsers
            html = resolveNamedHtmlEntities(html);

            // Ensure UTF-8 meta for browsers and downstream processors
            html = ensureUtf8Meta(html);
            // Force replace font families that commonly miss Vietnamese glyphs
            html = forceReplaceFontFamilies(html);

            // Normalize to XHTML-ish by self-closing void tags to satisfy XML parsers
            html = normalizeToXhtml(html);

            // Inject a simulated QR code into <div class="qr-code"></div>
            html = injectQrPlaceholder(html);

            // Sanitize any <img src> values that may contain raw '<' (e.g., data:image/svg+xml,<svg...>)
            html = sanitizeImgSrcAttributes(html);

            if (!StringUtils.hasText(html)) html = "<html><body>Không thể hiển thị nội dung mẫu</body></html>";
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        } catch (TransformerConfigurationException tce) {
            // Try to inject missing xsl:param then retry once
            String missing = extractUndefinedParam(tce.getMessage());
            if (StringUtils.hasText(missing)) {
                try {
                    TransformerFactory factory = TransformerFactory.newInstance();
                    try { factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true); } catch (Exception ignored) {}
                    try { factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file"); } catch (Exception ignored) {}
                    String injectedHtml = transformWithInjectedParams(factory, xsltValue, xsltFsPath, sampleXml, new java.util.LinkedHashSet<>(java.util.Arrays.asList(missing)));
                    if (!StringUtils.hasText(injectedHtml)) injectedHtml = "<html><body>Không thể hiển thị nội dung mẫu</body></html>";
                    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(injectedHtml);
                } catch (Exception retryEx) {
                    String msg = "<html><body>Lỗi xử lý mẫu (inject): " + escapeHtml(retryEx.getMessage()) + "</body></html>";
                    return ResponseEntity.status(500).contentType(MediaType.TEXT_HTML).body(msg);
                }
            }
            String msg = "<html><body>Lỗi cấu hình XSLT: " + escapeHtml(tce.getMessage()) + "</body></html>";
            return ResponseEntity.status(500).contentType(MediaType.TEXT_HTML).body(msg);
        } catch (Exception ex) {
            String msg = "<html><body>Lỗi xử lý mẫu: " + escapeHtml(ex.getMessage()) + "</body></html>";
            return ResponseEntity.status(500).contentType(MediaType.TEXT_HTML).body(msg);
        }
    }

    @GetMapping(value = "/{id}/download-xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> xmlDownload(@PathVariable Long id) {
        Optional<FormInvoiceEntity> opt = formInvoiceService.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN).body("Form không tồn tại");
        }
        FormInvoiceEntity e = opt.get();

        CompanyEntity company = null; CompanyBankEntity bank = null; LegalRepresentativeEntity rep = null; UserEntity user = null;
        if (e.getCompanyId() != null) {
            company = companyRepository.findById(e.getCompanyId()).orElse(null);
            if (company != null) {
                List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
                if (banks != null && !banks.isEmpty()) bank = banks.get(0);
            }
            rep = legalRepresentativeRepository.findByCompanyId(e.getCompanyId()).orElse(null);
        }
        if (e.getUserId() != null) user = userRepository.findById(e.getUserId()).orElse(null);
        String sampleXml = vn.hoadon.util.SampleInvoiceXmlBuilder.build(user, e, company, bank, rep);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("invoice-" + id + ".xml").build());
        return new ResponseEntity<>(sampleXml, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}/download-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> pdfDownload(@PathVariable Long id) {
        Optional<FormInvoiceEntity> opt = formInvoiceService.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN).body("Form không tồn tại");
        }
        FormInvoiceEntity e = opt.get();
        String xsltValue = e.getFile();
        if (!StringUtils.hasText(xsltValue)) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN).body("Không tìm thấy tệp XSLT của mẫu");
        }
        boolean looksLikeXslt = looksLikeInlineXslt(xsltValue);
        Path xsltFsPath = looksLikeXslt ? null : toFilesystemPath(xsltValue);
        if (!looksLikeXslt && (xsltFsPath == null || !Files.exists(xsltFsPath))) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN).body("Không tồn tại tệp XSLT trên hệ thống");
        }

        CompanyEntity company = null; CompanyBankEntity bank = null; LegalRepresentativeEntity rep = null; UserEntity user = null;
        if (e.getCompanyId() != null) {
            company = companyRepository.findById(e.getCompanyId()).orElse(null);
            if (company != null) {
                List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
                if (banks != null && !banks.isEmpty()) bank = banks.get(0);
            }
            rep = legalRepresentativeRepository.findByCompanyId(e.getCompanyId()).orElse(null);
        }
        if (e.getUserId() != null) user = userRepository.findById(e.getUserId()).orElse(null);
        String sampleXml = vn.hoadon.util.SampleInvoiceXmlBuilder.build(user, e, company, bank, rep);

        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            try { factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true); } catch (Exception ignored) {}
            try { factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file"); } catch (Exception ignored) {}

            StreamSource xsltSource = looksLikeXslt
                    ? new StreamSource(new StringReader(xsltValue))
                    : new StreamSource(xsltFsPath.toFile());
            Transformer transformer = factory.newTransformer(xsltSource);

            StreamSource xmlSource = new StreamSource(new StringReader(sampleXml));
            StringWriter outWriter = new StringWriter();
            StreamResult result = new StreamResult(outWriter);
            transformer.transform(xmlSource, result);
            String html = outWriter.toString();

            // Decode any HTML named entities to ensure XHTML compatibility for PDF renderer
            html = resolveNamedHtmlEntities(html);
            // Inject UTF-8 meta and robust font fallbacks to avoid missing glyphs
            html = ensureUtf8Meta(html);
            html = forceReplaceFontFamilies(html);
            // Normalize to XHTML-ish by self-closing void tags like <meta>, <link>, <img>, etc.
            html = normalizeToXhtml(html);
            html = injectQrPlaceholder(html);
            // Sanitize any <img src> values that may contain raw '<' before sending to renderer
            html = sanitizeImgSrcAttributes(html);

            // Inject layout fallbacks for CSS Grid and Flex (OpenHTMLToPDF limits)
            html = ensurePdfLayoutFallbackCss(html);

            // Ensure baseline PDF CSS so layout matches screen more closely if XSLT didn't define @page
            html = ensurePdfCss(html);
            // Also enforce a font-family that supports Vietnamese glyphs if none is specified
            html = ensurePdfFontCss(html);
            // Soften bold and improve text rendering
            html = ensurePdfTypoCss(html);

            // Convert HTML to PDF using OpenHTMLToPDF
            ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            String baseUri = null;
            if (!looksLikeXslt && xsltFsPath != null) {
                Path dir = xsltFsPath.getParent();
                if (dir != null) baseUri = dir.toUri().toString();
            }
            builder.useFastMode();
            // Try to force screen media so PDF matches the on-screen HTML (fallback if method isn't available)
            try {
                java.lang.reflect.Method m = PdfRendererBuilder.class.getMethod("useMediaType", String.class);
                m.invoke(builder, "screen");
            } catch (Exception ignore) {
                try {
                    java.lang.reflect.Method m2 = PdfRendererBuilder.class.getMethod("useScreenMediaType", boolean.class);
                    m2.invoke(builder, true);
                } catch (Exception ignore2) {}
            }
            // Use an URI resolver that can read from classpath:/static, absolute "/" and filesystem
            builder.useUriResolver(new ClasspathFirstUriResolver());

            // Register fonts that contain Vietnamese glyphs (Windows common fonts)
            registerAvailableUnicodeFonts(builder);

            if (baseUri != null) builder.withHtmlContent(html, baseUri); else builder.withHtmlContent(html, null);
            builder.toStream(pdfOut);
            builder.run();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentDisposition(ContentDisposition.attachment().filename("invoice-" + id + ".pdf").build());
            return new ResponseEntity<>(pdfOut.toByteArray(), headers, HttpStatus.OK);
        } catch (TransformerConfigurationException tce) {
            return ResponseEntity.status(500).contentType(MediaType.TEXT_PLAIN).body("Lỗi cấu hình XSLT: " + tce.getMessage());
        } catch (Exception ex) {
            return ResponseEntity.status(500).contentType(MediaType.TEXT_PLAIN).body("Lỗi tạo PDF: " + ex.getMessage());
        }
    }

    // New: Preview the exact HTML used to generate the PDF (helps ensure parity with /view)
    @GetMapping(value = "/{id}/view-pdf-html", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> viewPdfHtml(@PathVariable Long id) {
        Optional<FormInvoiceEntity> opt = formInvoiceService.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_HTML).body("<html><body>Form không tồn tại</body></html>");
        }
        FormInvoiceEntity e = opt.get();
        String xsltValue = e.getFile();
        if (!StringUtils.hasText(xsltValue)) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_HTML).body("<html><body>Không tìm thấy tệp XSLT của mẫu</body></html>");
        }
        boolean looksLikeXslt = looksLikeInlineXslt(xsltValue);
        Path xsltFsPath = looksLikeXslt ? null : toFilesystemPath(xsltValue);
        if (!looksLikeXslt && (xsltFsPath == null || !Files.exists(xsltFsPath))) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_HTML).body("<html><body>Không tồn tại tệp XSLT trên hệ thống</body></html>");
        }

        CompanyEntity company = null; CompanyBankEntity bank = null; LegalRepresentativeEntity rep = null; UserEntity user = null;
        if (e.getCompanyId() != null) {
            company = companyRepository.findById(e.getCompanyId()).orElse(null);
            if (company != null) {
                List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
                if (banks != null && !banks.isEmpty()) bank = banks.get(0);
            }
            rep = legalRepresentativeRepository.findByCompanyId(e.getCompanyId()).orElse(null);
        }
        if (e.getUserId() != null) user = userRepository.findById(e.getUserId()).orElse(null);
        String sampleXml = vn.hoadon.util.SampleInvoiceXmlBuilder.build(user, e, company, bank, rep);

        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            try { factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true); } catch (Exception ignored) {}
            try { factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file"); } catch (Exception ignored) {}
            StreamSource xsltSource = looksLikeXslt
                    ? new StreamSource(new StringReader(xsltValue))
                    : new StreamSource(xsltFsPath.toFile());
            Transformer transformer = factory.newTransformer(xsltSource);

            StreamSource xmlSource = new StreamSource(new StringReader(sampleXml));
            StringWriter outWriter = new StringWriter();
            StreamResult result = new StreamResult(outWriter);
            transformer.transform(xmlSource, result);
            String html = outWriter.toString();

            html = resolveNamedHtmlEntities(html);
            html = ensureUtf8Meta(html);
            html = forceReplaceFontFamilies(html);
            html = normalizeToXhtml(html);
            html = injectQrPlaceholder(html);
            html = sanitizeImgSrcAttributes(html);

            // Apply the same CSS adjustments used in PDF generation so this exactly matches the PDF input
            html = ensurePdfLayoutFallbackCss(html);
            html = ensurePdfCss(html);
            html = ensurePdfFontCss(html);
            html = ensurePdfTypoCss(html);

            if (!StringUtils.hasText(html)) html = "<html><body>Không thể hiển thị nội dung mẫu</body></html>";
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
        } catch (TransformerConfigurationException tce) {
            String msg = "<html><body>Lỗi cấu hình XSLT: " + escapeHtml(tce.getMessage()) + "</body></html>";
            return ResponseEntity.status(500).contentType(MediaType.TEXT_HTML).body(msg);
        } catch (Exception ex) {
            String msg = "<html><body>Lỗi xử lý mẫu: " + escapeHtml(ex.getMessage()) + "</body></html>";
            return ResponseEntity.status(500).contentType(MediaType.TEXT_HTML).body(msg);
        }
    }

    private boolean looksLikeInlineXslt(String content) {
        String s = content.trim();
        if (!s.startsWith("<")) return false;
        // Heuristic check for XSLT markers
        return s.contains("<xsl:stylesheet") || s.contains("xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"") || s.contains("http://www.w3.org/1999/XSL/Transform");
    }

    private String sampleQrDataUrl() {
        // Visible QR-like placeholder as inline SVG (no external assets required)
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'>"
                + "<rect width='100' height='100' fill='%23fff'/>"
                + "<rect x='8' y='8' width='24' height='24' fill='%23000'/>"
                + "<rect x='68' y='8' width='24' height='24' fill='%23000'/>"
                + "<rect x='8' y='68' width='24' height='24' fill='%23000'/>"
                + "<rect x='36' y='36' width='28' height='28' fill='%23000'/>"
                + "<text x='50' y='95' font-size='10' text-anchor='middle' fill='%23000'>QR</text>"
                + "</svg>";
        String base64 = java.util.Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return "data:image/svg+xml;base64," + base64;
    }

    private String sampleCodeCqt() {
        return "CQT123456789";
    }

    private String sampleStatus() {
        return "0";
    }

    private String sampleLookupCode() {
        return "LOOKUP-001";
    }

    private String extractUndefinedParam(String message) {
        if (!StringUtils.hasText(message)) return null;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("Variable or parameter '([^']+)' is undefined", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(message);
        if (m.find()) return m.group(1);
        p = java.util.regex.Pattern.compile("(variable|parameter)\\s+\\$?([a-zA-Z0-9_:-]+)\\s+undefined", java.util.regex.Pattern.CASE_INSENSITIVE);
        m = p.matcher(message);
        if (m.find()) return m.group(2);
        return null;
    }

    private String transformWithInjectedParams(TransformerFactory factory, String xsltValue, Path xsltFsPath, String sampleXml, java.util.Set<String> missingNames) throws Exception {
        // Load stylesheet text from inline value or filesystem
        String xsltText;
        if (looksLikeInlineXslt(xsltValue)) {
            xsltText = xsltValue;
        } else {
            xsltText = Files.readString(xsltFsPath);
        }
        int start = xsltText.indexOf("<xsl:stylesheet");
        if (start < 0) throw new IllegalStateException("Không tìm thấy thẻ xsl:stylesheet trong XSLT");
        int tagEnd = xsltText.indexOf('>', start);
        if (tagEnd < 0) throw new IllegalStateException("Không hợp lệ thẻ xsl:stylesheet");
        StringBuilder injected = new StringBuilder();
        injected.append(xsltText, 0, tagEnd + 1);
        injected.append("<xsl:param name=\"qrcode\"/>");
        injected.append("<xsl:param name=\"code_cqt\"/>");
        injected.append("<xsl:param name=\"status\"/>");
        injected.append("<xsl:param name=\"lookup_code\"/>");
        for (String name : missingNames) {
            if (name == null) continue;
            String n = name.trim();
            if (n.isEmpty()) continue;
            if ("qrcode".equals(n) || "code_cqt".equals(n) || "status".equals(n) || "lookup_code".equals(n)) continue;
            injected.append("<xsl:param name=\"" + n.replace("\"", "&quot;") + "\"/>");
        }
        injected.append(xsltText.substring(tagEnd + 1));

        // Perform transform
        StreamSource xsltSource = new StreamSource(new StringReader(injected.toString()));
        Transformer transformer = factory.newTransformer(xsltSource);
        for (String name : missingNames) {
            transformer.setParameter(name, "");
        }
        StreamSource xmlSource = new StreamSource(new StringReader(sampleXml));
        StringWriter outWriter = new StringWriter();
        StreamResult result = new StreamResult(outWriter);
        transformer.transform(xmlSource, result);
        String html = outWriter.toString();
        // Normalize named HTML entities to Unicode
        html = resolveNamedHtmlEntities(html);
        // Ensure void elements are self-closed for XML parsers
        html = normalizeToXhtml(html);
        html = injectQrPlaceholder(html);
        // Sanitize src attributes to avoid raw '<' characters
        return sanitizeImgSrcAttributes(html);
    }

    // Re-add missing helpers from original implementation
    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private Path toFilesystemPath(String publicPath) {
        if (!StringUtils.hasText(publicPath)) return null;
        String p = publicPath.trim();
        if (p.startsWith("/")) p = p.substring(1);
        if (p.contains("..")) {
            p = p.replace("..", "");
        }
        Path base = Paths.get(System.getProperty("user.dir"));
        return base.resolve(p);
    }

    // Ensure void HTML elements are self-closed so the content is XML well-formed for parsers expecting XHTML
    private String normalizeToXhtml(String html) {
        if (html == null || html.isEmpty()) return html;
        String[] voidTags = {"meta","link","img","br","hr","input","source","track","area","base","col","embed","param","wbr"};
        for (String tag : voidTags) {
            String pattern = "(?i)<" + tag + "(\\b[^>]*?)(?<!/)>";
            java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher m = p.matcher(html);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String attrs = m.group(1);
                String repl = "<" + tag + attrs + "/>";
                m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(repl));
            }
            m.appendTail(sb);
            html = sb.toString();
        }
        return html;
    }

    // Insert a placeholder QR image into any empty <div class="qr-code"> placeholders
    private String injectQrPlaceholder(String html) {
        if (html == null || html.isBlank()) return html;
        String img = "<img src=\"" + sampleQrDataUrl() + "\" alt=\"QR\" style=\"width:100%;height:100%;object-fit:contain;display:block;\"/>";
        String replacement = "<div class=\"qr-code\">" + img + "</div>";
        String safeReplacement = java.util.regex.Matcher.quoteReplacement(replacement);
        // Replace self-closing divs
        html = html.replaceAll("(?i)<div\\s+class=\\\"qr-code\\\"\\s*/>", safeReplacement);
        // Replace empty divs
        html = html.replaceAll("(?i)<div\\s+class=\\\"qr-code\\\"[^>]*>\\s*</div>", safeReplacement);
        return html;
    }

    // Fix malformed regex literal in resolveNamedHtmlEntities
    private String resolveNamedHtmlEntities(String html) {
        if (html == null || html.isEmpty()) return html;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("&([a-zA-Z][a-zA-Z0-9]+);");
        java.util.regex.Matcher m = p.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            // preserve XML built-ins
            if ("amp".equals(name) || "lt".equals(name) || "gt".equals(name) || "quot".equals(name) || "apos".equals(name)) {
                m.appendReplacement(sb, m.group(0));
                continue;
            }
            String repl = HTML_ENTITY_TO_UNICODE.get(name);
            if (repl == null) {
                // Fallback: replace unknown named entity with a plain space to avoid XML parser errors
                repl = " ";
            }
            // Quote replacement for regex
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // Minimal mapping for common HTML named entities to Unicode characters
    private static final java.util.Map<String, String> HTML_ENTITY_TO_UNICODE = new java.util.HashMap<>();
    static {
        // Latin-1 letters and punctuation commonly seen
        HTML_ENTITY_TO_UNICODE.put("nbsp", "\u00A0");
        HTML_ENTITY_TO_UNICODE.put("iexcl", "\u00A1");
        HTML_ENTITY_TO_UNICODE.put("cent", "\u00A2");
        HTML_ENTITY_TO_UNICODE.put("pound", "\u00A3");
        HTML_ENTITY_TO_UNICODE.put("curren", "\u00A4");
        HTML_ENTITY_TO_UNICODE.put("yen", "\u00A5");
        HTML_ENTITY_TO_UNICODE.put("brvbar", "\u00A6");
        HTML_ENTITY_TO_UNICODE.put("sect", "\u00A7");
        HTML_ENTITY_TO_UNICODE.put("uml", "\u00A8");
        HTML_ENTITY_TO_UNICODE.put("copy", "\u00A9");
        HTML_ENTITY_TO_UNICODE.put("ordf", "\u00AA");
        HTML_ENTITY_TO_UNICODE.put("laquo", "\u00AB");
        HTML_ENTITY_TO_UNICODE.put("not", "\u00AC");
        HTML_ENTITY_TO_UNICODE.put("shy", "\u00AD");
        HTML_ENTITY_TO_UNICODE.put("reg", "\u00AE");
        HTML_ENTITY_TO_UNICODE.put("macr", "\u00AF");
        HTML_ENTITY_TO_UNICODE.put("deg", "\u00B0");
        HTML_ENTITY_TO_UNICODE.put("plusmn", "\u00B1");
        HTML_ENTITY_TO_UNICODE.put("sup2", "\u00B2");
        HTML_ENTITY_TO_UNICODE.put("sup3", "\u00B3");
        HTML_ENTITY_TO_UNICODE.put("acute", "\u00B4");
        HTML_ENTITY_TO_UNICODE.put("micro", "\u00B5");
        HTML_ENTITY_TO_UNICODE.put("para", "\u00B6");
        HTML_ENTITY_TO_UNICODE.put("middot", "\u00B7");
        HTML_ENTITY_TO_UNICODE.put("cedil", "\u00B8");
        HTML_ENTITY_TO_UNICODE.put("sup1", "\u00B9");
        HTML_ENTITY_TO_UNICODE.put("ordm", "\u00BA");
        HTML_ENTITY_TO_UNICODE.put("raquo", "\u00BB");
        HTML_ENTITY_TO_UNICODE.put("frac14", "\u00BC");
        HTML_ENTITY_TO_UNICODE.put("frac12", "\u00BD");
        HTML_ENTITY_TO_UNICODE.put("frac34", "\u00BE");
        HTML_ENTITY_TO_UNICODE.put("iquest", "\u00BF");
        HTML_ENTITY_TO_UNICODE.put("Agrave", "\u00C0");
        HTML_ENTITY_TO_UNICODE.put("Aacute", "\u00C1");
        HTML_ENTITY_TO_UNICODE.put("Acirc", "\u00C2");
        HTML_ENTITY_TO_UNICODE.put("Atilde", "\u00C3");
        HTML_ENTITY_TO_UNICODE.put("Auml", "\u00C4");
        HTML_ENTITY_TO_UNICODE.put("Aring", "\u00C5");
        HTML_ENTITY_TO_UNICODE.put("AElig", "\u00C6");
        HTML_ENTITY_TO_UNICODE.put("Ccedil", "\u00C7");
        HTML_ENTITY_TO_UNICODE.put("Egrave", "\u00C8");
        HTML_ENTITY_TO_UNICODE.put("Eacute", "\u00C9");
        HTML_ENTITY_TO_UNICODE.put("Ecirc", "\u00CA");
        HTML_ENTITY_TO_UNICODE.put("Euml", "\u00CB");
        HTML_ENTITY_TO_UNICODE.put("Igrave", "\u00CC");
        HTML_ENTITY_TO_UNICODE.put("Iacute", "\u00CD");
        HTML_ENTITY_TO_UNICODE.put("Icirc", "\u00CE");
        HTML_ENTITY_TO_UNICODE.put("Iuml", "\u00CF");
        HTML_ENTITY_TO_UNICODE.put("ETH", "\u00D0");
        HTML_ENTITY_TO_UNICODE.put("Ntilde", "\u00D1");
        HTML_ENTITY_TO_UNICODE.put("Ograve", "\u00D2");
        HTML_ENTITY_TO_UNICODE.put("Oacute", "\u00D3");
        HTML_ENTITY_TO_UNICODE.put("Ocirc", "\u00D4");
        HTML_ENTITY_TO_UNICODE.put("Otilde", "\u00D5");
        HTML_ENTITY_TO_UNICODE.put("Ouml", "\u00D6");
        HTML_ENTITY_TO_UNICODE.put("times", "\u00D7");
        HTML_ENTITY_TO_UNICODE.put("Oslash", "\u00D8");
        HTML_ENTITY_TO_UNICODE.put("Ugrave", "\u00D9");
        HTML_ENTITY_TO_UNICODE.put("Uacute", "\u00DA");
        HTML_ENTITY_TO_UNICODE.put("Ucirc", "\u00DB");
        HTML_ENTITY_TO_UNICODE.put("Uuml", "\u00DC");
        HTML_ENTITY_TO_UNICODE.put("Yacute", "\u00DD");
        HTML_ENTITY_TO_UNICODE.put("THORN", "\u00DE");
        HTML_ENTITY_TO_UNICODE.put("szlig", "\u00DF");
        HTML_ENTITY_TO_UNICODE.put("agrave", "\u00E0");
        HTML_ENTITY_TO_UNICODE.put("aacute", "\u00E1");
        HTML_ENTITY_TO_UNICODE.put("acirc", "\u00E2");
        HTML_ENTITY_TO_UNICODE.put("atilde", "\u00E3");
        HTML_ENTITY_TO_UNICODE.put("auml", "\u00E4");
        HTML_ENTITY_TO_UNICODE.put("aring", "\u00E5");
        HTML_ENTITY_TO_UNICODE.put("aelig", "\u00E6");
        HTML_ENTITY_TO_UNICODE.put("ccedil", "\u00E7");
        HTML_ENTITY_TO_UNICODE.put("egrave", "\u00E8");
        HTML_ENTITY_TO_UNICODE.put("eacute", "\u00E9");
        HTML_ENTITY_TO_UNICODE.put("ecirc", "\u00EA");
        HTML_ENTITY_TO_UNICODE.put("euml", "\u00EB");
        HTML_ENTITY_TO_UNICODE.put("igrave", "\u00EC");
        HTML_ENTITY_TO_UNICODE.put("iacute", "\u00ED");
        HTML_ENTITY_TO_UNICODE.put("icirc", "\u00EE");
        HTML_ENTITY_TO_UNICODE.put("iuml", "\u00EF");
        HTML_ENTITY_TO_UNICODE.put("eth", "\u00F0");
        HTML_ENTITY_TO_UNICODE.put("ntilde", "\u00F1");
        HTML_ENTITY_TO_UNICODE.put("ograve", "\u00F2");
        HTML_ENTITY_TO_UNICODE.put("oacute", "\u00F3");
        HTML_ENTITY_TO_UNICODE.put("ocirc", "\u00F4");
        HTML_ENTITY_TO_UNICODE.put("otilde", "\u00F5");
        HTML_ENTITY_TO_UNICODE.put("ouml", "\u00F6");
        HTML_ENTITY_TO_UNICODE.put("divide", "\u00F7");
        HTML_ENTITY_TO_UNICODE.put("oslash", "\u00F8");
        HTML_ENTITY_TO_UNICODE.put("ugrave", "\u00F9");
        HTML_ENTITY_TO_UNICODE.put("uacute", "\u00FA");
        HTML_ENTITY_TO_UNICODE.put("ucirc", "\u00FB");
        HTML_ENTITY_TO_UNICODE.put("uuml", "\u00FC");
        HTML_ENTITY_TO_UNICODE.put("yacute", "\u00FD");
        HTML_ENTITY_TO_UNICODE.put("thorn", "\u00FE");
        HTML_ENTITY_TO_UNICODE.put("yuml", "\u00FF");
        // punctuation and symbols commonly seen
        HTML_ENTITY_TO_UNICODE.put("ndash", "\u2013");
        HTML_ENTITY_TO_UNICODE.put("mdash", "\u2014");
        HTML_ENTITY_TO_UNICODE.put("lsquo", "\u2018");
        HTML_ENTITY_TO_UNICODE.put("rsquo", "\u2019");
        HTML_ENTITY_TO_UNICODE.put("ldquo", "\u201C");
        HTML_ENTITY_TO_UNICODE.put("rdquo", "\u201D");
        HTML_ENTITY_TO_UNICODE.put("hellip", "\u2026");
        HTML_ENTITY_TO_UNICODE.put("bull", "\u2022");
        HTML_ENTITY_TO_UNICODE.put("euro", "\u20AC");
        HTML_ENTITY_TO_UNICODE.put("trade", "\u2122");
        // math
        HTML_ENTITY_TO_UNICODE.put("alpha", "\u03B1");
        HTML_ENTITY_TO_UNICODE.put("beta", "\u03B2");
        HTML_ENTITY_TO_UNICODE.put("gamma", "\u03B3");
        // angle quotes already added as laquo/raquo
    }

    // New: sanitize <img src> attributes that include raw '<' characters by encoding or converting to base64
    private String sanitizeImgSrcAttributes(String html) {
        if (html == null || html.isEmpty()) return html;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(?i)(<img\\b[^>]*?\\bsrc=)([\"'])(.+?)(\\2)", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher m = p.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String prefix = m.group(1);
            String quote = m.group(2);
            String src = m.group(3);
            String safeSrc = src;
            if (src.indexOf('<') >= 0) {
                String lower = src.toLowerCase();
                if (lower.startsWith("data:image/svg+xml")) {
                    int comma = src.indexOf(',');
                    if (comma > 0) {
                        String svgPayload = src.substring(comma + 1);
                        // Convert the SVG payload to base64
                        String base64 = java.util.Base64.getEncoder().encodeToString(svgPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        safeSrc = "data:image/svg+xml;base64," + base64;
                    } else {
                        // No comma found; percent-encode critical characters as a fallback
                        safeSrc = percentEncodeAngleBrackets(src);
                    }
                } else {
                    // Not an SVG data URI; percent-encode critical characters
                    safeSrc = percentEncodeAngleBrackets(src);
                }
            }
            String replacement = prefix + quote + safeSrc + quote;
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String percentEncodeAngleBrackets(String s) {
        if (s == null) return null;
        // Encode characters illegal inside URLs/attributes when unescaped
        return s.replace("<", "%3C")
                .replace(">", "%3E")
                .replace("\"", "%22")
                .replace("'", "%27")
                .replace(" ", "%20")
                .replace("#", "%23");
    }

    // Add a resolver so OpenHTMLToPDF can resolve resources (CSS/images) from classpath static and filesystem
    static class ClasspathFirstUriResolver implements FSUriResolver {
        private final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        @Override
        public String resolveURI(String baseUri, String uri) {
            if (uri == null || uri.isBlank()) return uri;
            String u = uri.trim();
            String lower = u.toLowerCase();
            // passthrough data/http/file URIs
            if (lower.startsWith("data:")) return u;
            if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("file:")) return u;

            // classpath: scheme
            if (lower.startsWith("classpath:")) {
                String path = u.substring("classpath:".length());
                if (path.startsWith("/")) path = path.substring(1);
                String[] candidates = new String[] { path, "static/" + path, "public/" + path, "templates/" + path };
                for (String c : candidates) {
                    java.net.URL url = cl.getResource(c);
                    if (url != null) return url.toExternalForm();
                }
                return u; // fallback
            }

            // absolute path like /css/app.css -> try classpath static first, then filesystem under user.dir
            if (u.startsWith("/")) {
                String path = u.substring(1);
                String[] candidates = new String[] { "static/" + path, "public/" + path, path };
                for (String c : candidates) {
                    java.net.URL url = cl.getResource(c);
                    if (url != null) return url.toExternalForm();
                }
                try {
                    Path fs = Paths.get(System.getProperty("user.dir")).resolve(path);
                    if (Files.exists(fs)) return fs.toUri().toString();
                } catch (Exception ignore) {}
                return u;
            }

            // relative URI -> resolve against baseUri if present
            if (baseUri != null && !baseUri.isBlank()) {
                try { return URI.create(baseUri).resolve(u).toString(); } catch (Exception ignore) {}
            }
            // last resort: look up in classpath static
            String[] candidates = new String[] { u, "static/" + u, "public/" + u };
            for (String c : candidates) {
                java.net.URL url = cl.getResource(c);
                if (url != null) return url.toExternalForm();
            }
            // as a final fallback, try filesystem
            try {
                Path fs = Paths.get(System.getProperty("user.dir")).resolve(u);
                if (Files.exists(fs)) return fs.toUri().toString();
            } catch (Exception ignore) {}
            return u;
        }
    }

    // Ensure PDF-friendly CSS if the HTML lacks @page rules; also force print-color-adjust for color accuracy
    private String ensurePdfCss(String html) {
        if (html == null || html.isBlank()) return html;
        // If the template already defines @page, don't inject defaults
        if (html.matches("(?is).*@page\\s*\\{")) return html;
        String css = "<style>" +
                "@page { size: A4; margin: 10mm; }" +
                "html, body { print-color-adjust: exact; -webkit-print-color-adjust: exact; }" +
                "img { max-width: 100%; }" +
                "table { border-collapse: collapse; }" +
                "</style>";
        if (html.matches("(?is).*<head[^>]*>.*")) {
            return html.replaceFirst("(?is)(<head[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(css));
        }
        if (html.matches("(?is).*<html[^>]*>.*")) {
            return html.replaceFirst("(?is)(<html[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(css));
        }
        return css + html;
    }

    // New: inject font-family CSS and register fonts to fix missing Vietnamese glyphs in PDF output
    private String ensurePdfFontCss(String html) {
        if (html == null || html.isBlank()) return html;
        // Always inject a strong default font stack to ensure Unicode glyphs are available in PDF
        String fontCss = "<style>" +
                "@media all {" +
                "  * { font-family: 'DejaVu Sans', 'Noto Sans', 'Segoe UI', Tahoma, 'Arial Unicode MS', Arial, sans-serif !important; }" +
                "}" +
                "</style>";
        if (html.matches("(?is).*<head[^>]*>.*")) {
            html = html.replaceFirst("(?is)(<head[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(fontCss));
        } else if (html.matches("(?is).*<html[^>]*>.*")) {
            html = html.replaceFirst("(?is)(<html[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(fontCss));
        } else {
            html = fontCss + html;
        }
        return html;
    }

    // Inject CSS fallbacks for features not supported by OpenHTMLToPDF (e.g., CSS Grid)
    private String ensurePdfLayoutFallbackCss(String html) {
        if (html == null || html.isBlank()) return html;
        String css = "<style>" +
                "@media print, all {" +
                // header: emulate grid with table layout
                "  .header { display: table !important; width: 100%; border-bottom: 2px solid #000; margin-bottom: 12px; }" +
                "  .header > * { display: table-cell !important; vertical-align: middle; padding-bottom: 8px; }" +
                "  .header > :nth-child(1) { width: 80px; }" +
                "  .header > :nth-child(2) { width: auto; }" +
                "  .header > :nth-child(3) { white-space: nowrap; }" +
                "  .header > * { padding-right: 10px; }" +
                "  .header > :last-child { padding-right: 0; }" +
                // signatures: emulate flex with table cells spread across
                "  .signatures { display: table !important; width: 100%; margin-top: 20px; font-size: 11.5px; text-align: center; }" +
                "  .signatures > * { display: table-cell !important; text-align: center; vertical-align: top; }" +
                // buyer-line: replace flex row with table to keep vertical centering
                "  .buyer-line { display: table !important; width: 100%; margin-bottom: 2px; min-height: 18px; }" +
                "  .buyer-line > * { display: table-cell !important; vertical-align: middle; }" +
                // logo: avoid flex centering; use absolute centering inside a relative wrapper
                "  .logo { display: block !important; position: relative !important; }" +
                "  .logo-inner { display: block !important; position: absolute !important; top: 0; bottom: 0; left: 0; right: 0; margin: auto; text-align: center; }" +
                "}" +
                "</style>";
        if (html.matches("(?is).*<head[^>]*>.*")) {
            return html.replaceFirst("(?is)(<head[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(css));
        }
        if (html.matches("(?is).*<html[^>]*>.*")) {
            return html.replaceFirst("(?is)(<html[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(css));
        }
        return css + html;
    }

    private String ensurePdfTypoCss(String html) {
        if (html == null || html.isBlank()) return html;
        String css = "<style>" +
                "@media print, all {" +
                "  html, body { -webkit-font-smoothing: antialiased; text-rendering: optimizeLegibility; font-synthesis: none; }" +
                "  b, strong { font-weight: 600 !important; }" +
                "  [style*='font-weight:700'] { font-weight: 600 !important; }" +
                "  h1, h2, h3, h4, h5 { font-weight: 600 !important; }" +
                "}" +
                "</style>";
        if (html.matches("(?is).*<head[^>]*>.*")) {
            return html.replaceFirst("(?is)(<head[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(css));
        }
        if (html.matches("(?is).*<html[^>]*>.*")) {
            return html.replaceFirst("(?is)(<html[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(css));
        }
        return css + html;
    }

    private void registerAvailableUnicodeFonts(PdfRendererBuilder builder) {
        // Try Linux common fonts first
        try {
            Path dejavu = Paths.get("/usr/share/fonts/truetype/dejavu");
            registerFontIfExists(builder, dejavu, "DejaVuSans.ttf", "DejaVu Sans");
            registerFontIfExists(builder, dejavu, "DejaVuSans-Bold.ttf", "DejaVu Sans");
            registerFontIfExists(builder, dejavu, "DejaVuSansCondensed.ttf", "DejaVu Sans");
            registerFontIfExists(builder, dejavu, "DejaVuSansCondensed-Bold.ttf", "DejaVu Sans");
        } catch (Throwable ignore) {}
        try {
            Path noto = Paths.get("/usr/share/fonts/noto");
            registerFontIfExists(builder, noto, "NotoSans-Regular.ttf", "Noto Sans");
            registerFontIfExists(builder, noto, "NotoSans-Bold.ttf", "Noto Sans");
            registerFontIfExists(builder, noto, "NotoSansDisplay-Regular.ttf", "Noto Sans");
            registerFontIfExists(builder, noto, "NotoSansDisplay-Bold.ttf", "Noto Sans");
        } catch (Throwable ignore) {}
        // Existing Windows font registration
        String windowsFonts = System.getenv("WINDIR");
        Path fontsDir = null;
        if (windowsFonts != null && !windowsFonts.isBlank()) {
            fontsDir = Paths.get(windowsFonts, "Fonts");
        } else {
            fontsDir = Paths.get("C:", "Windows", "Fonts");
        }
        // candidates: Segoe UI, Tahoma, Arial Unicode MS
        registerFontIfExists(builder, fontsDir, "segoeui.ttf", "Segoe UI");
        registerFontIfExists(builder, fontsDir, "segoeuib.ttf", "Segoe UI");
        registerFontIfExists(builder, fontsDir, "tahoma.ttf", "Tahoma");
        registerFontIfExists(builder, fontsDir, "tahomabd.ttf", "Tahoma");
        registerFontIfExists(builder, fontsDir, "ARIALUNI.TTF", "Arial Unicode MS");
    }

    // Helper: register a font file with the PDF renderer if it exists at the given path
    private void registerFontIfExists(PdfRendererBuilder builder, Path fontsDir, String fileName, String family) {
        if (fontsDir == null) return;
        try {
            Path p = fontsDir.resolve(fileName);
            if (Files.exists(p)) {
                java.io.File f = p.toFile();
                builder.useFont(f, family);
            }
        } catch (Throwable ignore) {}
    }

    // Add UTF-8 meta tag if missing at the top of the document
    private String ensureUtf8Meta(String html) {
        if (html == null || html.isBlank()) return html;
        String meta = "<meta charset=\"UTF-8\"/>";
        if (html.matches("(?is).*<head[^>]*>.*")) {
            if (!html.matches("(?is).*<meta\\s+charset=\\\"?utf-8\\\"?.*")) {
                html = html.replaceFirst("(?is)(<head[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement(meta));
            }
            return html;
        }
        if (html.matches("(?is).*<html[^>]*>.*")) {
            return html.replaceFirst("(?is)(<html[^>]*>)", "$1" + java.util.regex.Matcher.quoteReplacement("<head>" + meta + "</head>"));
        }
        return "<head>" + meta + "</head>" + html;
    }

    // Replace known problematic font families with robust Unicode-capable defaults
    private String forceReplaceFontFamilies(String html) {
        if (html == null || html.isBlank()) return html;
        String[] badFonts = new String[] { "Times New Roman", "Times", "Georgia", "Roboto", "Tahoma", "Arial", "Helvetica", "Calibri", "Cambria" };
        for (String f : badFonts) {
            String patternCss = "(?is)font-family\\s*:\\s*" + java.util.regex.Pattern.quote(f) + "(\\s*,[^;]*)?;";
            html = html.replaceAll(patternCss, "font-family: 'DejaVu Sans', 'Noto Sans', sans-serif;");
            String patternInline = "(?is)(font-family)(\\s*=\\s*)(\\\"|')" + java.util.regex.Pattern.quote(f) + "(\\3)";
            html = html.replaceAll(patternInline, "$1$2$3DejaVu Sans$3");
        }
        html = html.replaceAll("(?is)<font\\s+face=\\\"[^\\\"]+\\\"", "<font face=\"DejaVu Sans\"");
        return html;
    }
}
