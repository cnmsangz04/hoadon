package vn.hoadon.controllers.customers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.forminvoice.FormInvoiceListItemDto;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.CompanyBankEntity;
import vn.hoadon.entity.FormInvoiceEntity;
import vn.hoadon.entity.InvoiceNumberEntity;
import vn.hoadon.entity.LegalRepresentativeEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.CompanyBankRepository;
import vn.hoadon.repositories.LegalRepresentativeRepository;
import vn.hoadon.repositories.InvoiceRepository;
import vn.hoadon.services.FormInvoiceService;
import vn.hoadon.services.InvoiceNumberService;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.UUID;

import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ContentDisposition;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/v1/form-invoices")
public class FormInvoiceController extends BaseController {

    private final FormInvoiceService service;
    private final InvoiceNumberService invoiceNumberService;

    @Autowired private UserRepository userRepository;
    @Autowired private CompanyRepository companyRepository;
    @Autowired private CompanyBankRepository companyBankRepository;
    @Autowired private LegalRepresentativeRepository legalRepresentativeRepository;
    @Autowired private InvoiceRepository invoiceRepository;

    @Autowired
    public FormInvoiceController(FormInvoiceService service, InvoiceNumberService invoiceNumberService) {
        this.service = service;
        this.invoiceNumberService = invoiceNumberService;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer status
    ) {
        UserEntity user = currentUser();
        Long companyId = user != null ? user.getCompanyId() : null;
        if (companyId == null) {
            return empty(page, size);
        }
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<FormInvoiceEntity> p = service.pageByCompanySystem(companyId, 1, pageable);

        List<FormInvoiceEntity> filtered = p.getContent();
        if (q != null && !q.isBlank()) {
            String kw = q.toLowerCase();
            filtered = filtered.stream().filter(it -> {
                String name = it.getName() != null ? it.getName().toLowerCase() : "";
                String combinedSerial = ((it.getFormCode() != null ? it.getFormCode() : "") + (it.getSerial() != null ? it.getSerial() : "")).toLowerCase();
                return name.contains(kw) || combinedSerial.contains(kw);
            }).toList();
        }
        if (category != null) {
            filtered = filtered.stream().filter(it -> Objects.equals(category, it.getCategory())).toList();
        }
        if (type != null) {
            filtered = filtered.stream().filter(it -> Objects.equals(type, it.getType())).toList();
        }
        if (status != null) {
            filtered = filtered.stream().filter(it -> Objects.equals(status, it.getStatus())).toList();
        }

        // Explicitly sort by id desc (nulls last) after filtering
        filtered = filtered.stream()
                .sorted(Comparator.comparing(FormInvoiceEntity::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        // Resolve usernames for displayed items
        Set<Long> uidSet = new HashSet<>();
        for (FormInvoiceEntity it : filtered) {
            if (it.getUserId() != null) uidSet.add(it.getUserId());
        }
        Map<Long, String> usernameMap = new HashMap<>();
        if (!uidSet.isEmpty()) {
            List<UserEntity> users = userRepository.findAllById(uidSet);
            for (UserEntity u : users) {
                if (u.getId() != null) {
                    usernameMap.put(u.getId(), Optional.ofNullable(u.getUsername()).orElse(u.getName()));
                }
            }
        }

        List<FormInvoiceListItemDto> items = mapToDto(filtered, usernameMap);
        Map<String, Object> res = new HashMap<>();
        res.put("items", items);
        res.put("total", p.getTotalElements());
        res.put("per_page", p.getSize());
        res.put("current_page", p.getNumber() + 1);
        res.put("last_page", Math.max(1, p.getTotalPages()));
        return res;
    }

    @GetMapping("/templates")
    public Map<String, Object> listTemplates(
            @RequestParam(required = false) Integer category,
            @RequestParam(required = false) Integer type,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        UserEntity user = currentUser();
        Long companyId = user != null ? user.getCompanyId() : null;
        if (companyId == null) return empty(page, size);
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<FormInvoiceEntity> p = service.pageByCompanySystem(companyId, 0, pageable);
        List<FormInvoiceEntity> filtered = p.getContent();
        if (category != null) filtered = filtered.stream().filter(it -> Objects.equals(category, it.getCategory())).toList();
        if (type != null) filtered = filtered.stream().filter(it -> Objects.equals(type, it.getType())).toList();
        // Sort templates by id desc (nulls last)
        filtered = filtered.stream()
                .sorted(Comparator.comparing(FormInvoiceEntity::getId,
                        Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();
        List<Map<String, Object>> items = filtered.stream().map(it -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", it.getId());
            m.put("name", it.getName());
            m.put("photo", it.getPhoto());
            m.put("type", it.getType());
            m.put("category", it.getCategory());
            return m;
        }).toList();
        Map<String, Object> res = new HashMap<>();
        res.put("items", items);
        res.put("total", p.getTotalElements());
        res.put("per_page", p.getSize());
        res.put("current_page", p.getNumber() + 1);
        res.put("last_page", Math.max(1, p.getTotalPages()));
        return res;
    }

    @GetMapping("/templates/{id}")
    public ResponseEntity<?> getTemplate(@PathVariable Long id) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        // Restrict to same company and system templates only
        if (!Objects.equals(e.getCompanyId(), user.getCompanyId()) || !Objects.equals(e.getSystem(), 0)) {
            return ResponseEntity.status(403).build();
        }
        Map<String, Object> item = new HashMap<>();
        item.put("id", e.getId());
        item.put("name", e.getName());
        item.put("serial", e.getSerial());
        item.put("file", e.getFile());
        item.put("photo", e.getPhoto());
        item.put("type", e.getType());
        item.put("category", e.getCategory());
        return ResponseEntity.ok(item);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> detail(@PathVariable Long id) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (!Objects.equals(e.getCompanyId(), user.getCompanyId())) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(e);
    }

    // --- Render HTML preview by applying XSLT template to sample XML ---
    @GetMapping(value = "/{id}/view", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<?> viewHtml(@PathVariable Long id) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();

        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (!Objects.equals(e.getCompanyId(), user.getCompanyId())) return ResponseEntity.status(403).build();

        String xsltPublicPath = e.getFile();
        if (!StringUtils.hasText(xsltPublicPath)) {
            return ResponseEntity.status(404).body("<html><body>Không tìm thấy tệp XSLT của mẫu</body></html>");
        }
        Path xsltFsPath = toFilesystemPath(xsltPublicPath);
        if (xsltFsPath == null || !Files.exists(xsltFsPath)) {
            return ResponseEntity.status(404).body("<html><body>Không tồn tại tệp XSLT trên hệ thống</body></html>");
        }

        // Build sample XML using SampleInvoiceXmlBuilder with mapped data
        CompanyEntity company = null;
        CompanyBankEntity bank = null;
        LegalRepresentativeEntity rep = null;
        if (e.getCompanyId() != null) {
            company = companyRepository.findById(e.getCompanyId()).orElse(null);
            if (company != null) {
                java.util.List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
                if (banks != null && !banks.isEmpty()) bank = banks.get(0);
            }
            rep = legalRepresentativeRepository.findByCompanyId(e.getCompanyId()).orElse(null);
        }
        String sampleXml = vn.hoadon.util.SampleInvoiceXmlBuilder.build(user, e, company, bank, rep);

        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            // Enable secure processing when available
            try { factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true); } catch (Exception ignored) {}
            // Allow local file includes/imports if the stylesheet uses them
            try { factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file"); } catch (Exception ignored) {}

            StreamSource xsltSource = new StreamSource(xsltFsPath.toFile());
            Transformer transformer = factory.newTransformer(xsltSource);
            // Provide common default parameters expected by templates
            transformer.setParameter("qrcode", sampleQrDataUrl());
            transformer.setParameter("code_cqt", sampleCodeCqt());
            transformer.setParameter("status", sampleStatus());
            transformer.setParameter("lookup_code", sampleLookupCode());

            StreamSource xmlSource = new StreamSource(new StringReader(sampleXml));
            StringWriter outWriter = new StringWriter();
            StreamResult result = new StreamResult(outWriter);
            transformer.transform(xmlSource, result);
            String html = outWriter.toString();

            if (!StringUtils.hasText(html)) html = "<html><body>Không thể hiển thị nội dung mẫu</body></html>";
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(html);
        } catch (TransformerConfigurationException tce) {
            // Attempt to inject missing xsl:param declarations then retry
            String missing = extractUndefinedParam(tce.getMessage());
            if (StringUtils.hasText(missing)) {
                try {
                    TransformerFactory factory = TransformerFactory.newInstance();
                    try { factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true); } catch (Exception ignored) {}
                    try { factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file"); } catch (Exception ignored) {}
                    String html = transformWithInjectedParams(factory, xsltFsPath, sampleXml, new java.util.LinkedHashSet<>(java.util.Arrays.asList(missing)));
                    if (!StringUtils.hasText(html)) html = "<html><body>Không thể hiển thị nội dung mẫu</body></html>";
                    return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
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

    private String sampleQrDataUrl() {
        // Tiny 1x1 transparent PNG data URL as a safe placeholder QR image
        return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGMAAQAABQABDQottQAAAABJRU5ErkJggg==";
    }

    private String sampleCodeCqt() {
        return "CQT123456789";
    }

    private String sampleStatus() {
        // Use numeric status default per requirement
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

    private String transformWithInjectedParams(TransformerFactory factory, Path originalXsltPath, String sampleXml, java.util.Set<String> missingNames) throws Exception {
        String xsltText = Files.readString(originalXsltPath);
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

        StreamSource xsltSource = new StreamSource(new StringReader(injected.toString()));
        Transformer transformer = factory.newTransformer(xsltSource);
        transformer.setParameter("qrcode", sampleQrDataUrl());
        transformer.setParameter("code_cqt", sampleCodeCqt());
        transformer.setParameter("status", sampleStatus());
        transformer.setParameter("lookup_code", sampleLookupCode());
        for (String name : missingNames) {
            transformer.setParameter(name, "");
        }
        StreamSource xmlSource = new StreamSource(new StringReader(sampleXml));
        StringWriter outWriter = new StringWriter();
        StreamResult result = new StreamResult(outWriter);
        transformer.transform(xmlSource, result);
        return outWriter.toString();
    }
    
    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(403).body(Map.of("message", "Không xác định được công ty"));
        }
        Long companyId = user.getCompanyId();

        String name = Optional.ofNullable(body.get("name")).map(Object::toString).orElse(null);
        String serialFull = Optional.ofNullable(body.get("serial")).map(Object::toString).orElse(null);
        Integer category = Optional.ofNullable(body.get("category")).map(v -> Integer.valueOf(v.toString())).orElse(null);
        Integer type = Optional.ofNullable(body.get("type")).map(v -> Integer.valueOf(v.toString())).orElse(null);
        Integer status = Optional.ofNullable(body.get("status")).map(v -> Integer.valueOf(v.toString())).orElse(1);
        // Always set system=1 for customer-created samples
        Integer system = 1;
        String file = Optional.ofNullable(body.get("file")).map(Object::toString).orElse(null);
        String photo = Optional.ofNullable(body.get("photo")).map(Object::toString).orElse(null);
        Long templateId = Optional.ofNullable(body.get("templateId")).map(v -> Long.valueOf(v.toString())).orElse(null);
        Integer haveCode = Optional.ofNullable(body.get("have_code")).map(v -> Integer.valueOf(v.toString())).orElse(0);

        // Validate name
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Tên mẫu không được để trống"));
        }
        // Validate serial (full): must have at least 2 chars to split
        if (serialFull == null || serialFull.trim().length() < 2) {
            return ResponseEntity.badRequest().body(Map.of("message", "Ký hiệu không hợp lệ"));
        }
        serialFull = serialFull.trim();
        String formCode = serialFull.substring(0, 1);
        String serialRemainder = serialFull.substring(1);

        // If creating from a system template, copy its file/photo into company uploads and use those new paths
        if (templateId != null) {
            Optional<FormInvoiceEntity> optTpl = service.findById(templateId);
            if (optTpl.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("message", "Không tìm thấy mẫu template"));
            }
            FormInvoiceEntity tpl = optTpl.get();
            if (!Objects.equals(tpl.getCompanyId(), companyId) || !Objects.equals(tpl.getSystem(), 0)) {
                return ResponseEntity.status(403).body(Map.of("message", "Chỉ được sao chép từ mẫu hệ thống của công ty"));
            }
            // Copy photo
            if (StringUtils.hasText(tpl.getPhoto())) {
                String copiedPhoto = copyToCompanyUploads(tpl.getPhoto(), companyId, "photo");
                if (copiedPhoto != null) {
                    photo = copiedPhoto;
                }
            }
            // Copy file (xslt/template file)
            if (StringUtils.hasText(tpl.getFile())) {
                String copiedFile = copyToCompanyUploads(tpl.getFile(), companyId, "template");
                if (copiedFile != null) {
                    file = copiedFile;
                }
            }
        }

        FormInvoiceEntity e = new FormInvoiceEntity();
        e.setCompanyId(companyId);
        e.setUserId(user.getId());
        e.setName(name);
        e.setFormCode(formCode);
        e.setSerial(serialRemainder);
        e.setCategory(category);
        e.setType(type);
        e.setStatus(status);
        e.setSystem(system);
        e.setFile(file);
        e.setPhoto(photo);
        e.setHaveCode(haveCode);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());

        FormInvoiceEntity saved;
        try {
            saved = service.create(e);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        }

        // Ensure only one active (status=1) per category for customer samples (system=1)
        if (Objects.equals(saved.getSystem(), 1) && Objects.equals(saved.getStatus(), 1) && saved.getCategory() != null) {
            deactivateOthersActive(companyId, saved.getCategory(), saved.getId());
        }

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", saved.getId());
        resp.put("message", "Đã tạo mẫu hóa đơn");
        return ResponseEntity.ok(resp);
    }

    // --- Helpers for uploads path resolving and copying ---
    /**
     * Copy a file from an existing public uploads path to the company's uploads subfolder and return the new public path.
     * Subfolder: "photo" or "template".
     */
    private String copyToCompanyUploads(String sourcePublicPath, Long companyId, String subfolder) {
        try {
            Path srcFs = toFilesystemPath(sourcePublicPath);
            if (srcFs == null || !Files.exists(srcFs)) return null;

            String ext = getExtension(srcFs.getFileName().toString());
            String baseName = ("template".equals(subfolder) ? "template_" : "photo_") + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 6);
            String targetFileName = ext != null && !ext.isBlank() ? baseName + "." + ext : baseName;

            Path companyDir = getCompanyUploadsDir(companyId, subfolder);
            if (!Files.exists(companyDir)) Files.createDirectories(companyDir);
            Path targetFs = companyDir.resolve(targetFileName);
            Files.copy(srcFs, targetFs);
            return toPublicPath(targetFs);
        } catch (IOException ex) {
            return null;
        }
    }

    /** Resolve a public uploads path like "/uploads/{companyId}/..." to a filesystem path. */
    private Path toFilesystemPath(String publicPath) {
        if (!StringUtils.hasText(publicPath)) return null;
        String p = publicPath.trim();
        if (p.startsWith("/")) p = p.substring(1);
        // Base dir is application working directory
        Path base = Paths.get(System.getProperty("user.dir"));
        return base.resolve(p);
    }

    /** Convert a filesystem path under the application's working dir to a public uploads path starting with "/uploads/". */
    private String toPublicPath(Path fsPath) {
        Path base = Paths.get(System.getProperty("user.dir"));
        Path rel = base.relativize(fsPath);
        String s = rel.toString().replace('\\', '/');
        if (!s.startsWith("uploads/")) s = "uploads/" + s;
        return "/" + s;
    }

    /** Get the company's uploads directory for a given subfolder. */
    private Path getCompanyUploadsDir(Long companyId, String subfolder) {
        Path base = Paths.get(System.getProperty("user.dir"));
        return base.resolve(Paths.get("uploads", String.valueOf(companyId), subfolder));
    }

    /** Extract file extension without dot. */
    private String getExtension(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return null;
        return filename.substring(idx + 1);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity ex = opt.get();
        if (!Objects.equals(ex.getCompanyId(), user.getCompanyId())) return ResponseEntity.status(403).build();

        FormInvoiceEntity patch = new FormInvoiceEntity();
        patch.setName(Optional.ofNullable(body.get("name")).map(Object::toString).orElse(null));
        String serialFull = Optional.ofNullable(body.get("serial")).map(Object::toString).orElse(null);
        if (serialFull != null) {
            String s = serialFull.trim();
            if (s.length() >= 2) {
                patch.setFormCode(s.substring(0,1));
                patch.setSerial(s.substring(1));
            } else {
                return ResponseEntity.badRequest().body(Map.of("message", "Ký hiệu không hợp lệ"));
            }
        }
        patch.setCategory(Optional.ofNullable(body.get("category")).map(v -> Integer.valueOf(v.toString())).orElse(null));
        patch.setType(Optional.ofNullable(body.get("type")).map(v -> Integer.valueOf(v.toString())).orElse(null));
        patch.setStatus(Optional.ofNullable(body.get("status")).map(v -> Integer.valueOf(v.toString())).orElse(null));
        patch.setSystem(Optional.ofNullable(body.get("system")).map(v -> Integer.valueOf(v.toString())).orElse(null));
        patch.setFile(Optional.ofNullable(body.get("file")).map(Object::toString).orElse(null));
        patch.setPhoto(Optional.ofNullable(body.get("photo")).map(Object::toString).orElse(null));
        patch.setHaveCode(Optional.ofNullable(body.get("have_code")).map(v -> Integer.valueOf(v.toString())).orElse(null));

        Optional<FormInvoiceEntity> updated;
        try {
            updated = service.update(id, patch);
        } catch (IllegalArgumentException iae) {
            return ResponseEntity.badRequest().body(Map.of("message", iae.getMessage()));
        }
        if (updated.isEmpty()) return ResponseEntity.status(500).body(Map.of("message", "Cập nhật thất bại"));

        // After update, if record is customer sample and status=1, deactivate others in same category
        FormInvoiceEntity cur = updated.get();
        Integer effectiveSystem = cur.getSystem() != null ? cur.getSystem() : ex.getSystem();
        Integer effectiveStatus = cur.getStatus() != null ? cur.getStatus() : ex.getStatus();
        Integer effectiveCategory = cur.getCategory() != null ? cur.getCategory() : ex.getCategory();
        if (Objects.equals(effectiveSystem, 1) && Objects.equals(effectiveStatus, 1) && effectiveCategory != null) {
            deactivateOthersActive(user.getCompanyId(), effectiveCategory, cur.getId());
        }

        return ResponseEntity.ok(Map.of("message", "Đã cập nhật mẫu"));
    }

    /**
     * Deactivate (status=0) other active records within the same company and category, excluding keepId.
     */
    private void deactivateOthersActive(Long companyId, Integer category, Long keepId) {
        Pageable pageable = PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "updatedAt"));
        Page<FormInvoiceEntity> p = service.pageByCompanySystem(companyId, 1, pageable);
        for (FormInvoiceEntity it : p.getContent()) {
            if (Objects.equals(it.getCategory(), category) && !Objects.equals(it.getId(), keepId) && Objects.equals(it.getStatus(), 1)) {
                FormInvoiceEntity patch = new FormInvoiceEntity();
                patch.setStatus(0);
                service.update(it.getId(), patch);
            }
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (!Objects.equals(e.getCompanyId(), user.getCompanyId())) {
            return ResponseEntity.status(403).build();
        }
        // Prevent deleting if any invoice references this form
        try {
            Integer formIdInt = e.getId() != null ? e.getId().intValue() : null;
            if (formIdInt != null && invoiceRepository.existsByFormId(formIdInt)) {
                return ResponseEntity.status(400).build();
            }
        } catch (Exception ignore) {}
        // Only allow delete when status == 0
        if (!Objects.equals(e.getStatus(), 0)) {
            return ResponseEntity.status(400).build();
        }
        // Remove associated files (photo and file) if present
        try {
            String photo = e.getPhoto();
            if (StringUtils.hasText(photo)) {
                Path p = toFilesystemPath(photo);
                if (p != null) Files.deleteIfExists(p);
            }
        } catch (Exception ignore) {}
        try {
            String tplFile = e.getFile();
            if (StringUtils.hasText(tplFile)) {
                Path p = toFilesystemPath(tplFile);
                if (p != null) Files.deleteIfExists(p);
            }
        } catch (Exception ignore) {}
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users/by-ids")
    public List<Map<String, Object>> usersByIds(@RequestParam String ids) {
        if (ids == null || ids.isBlank()) return List.of();
        List<Long> idList = Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> s.matches("^\\d+$"))
                .map(Long::parseLong)
                .toList();
        if (idList.isEmpty()) return List.of();
        List<UserEntity> users = userRepository.findAllById(idList);
        return users.stream().map(u -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", u.getId());
            m.put("username", u.getUsername());
            m.put("name", u.getName());
            return m;
        }).toList();
    }

    private List<FormInvoiceListItemDto> mapToDto(List<FormInvoiceEntity> list, Map<Long, String> usernameMap) {
        return list.stream().map(it -> {
            FormInvoiceListItemDto d = new FormInvoiceListItemDto();
            d.setId(it.getId());
            d.setName(it.getName());
            // Use combined serial: formCode + serial
            String combinedSerial = (it.getFormCode() != null ? it.getFormCode() : "") + (it.getSerial() != null ? it.getSerial() : "");
            d.setSerial(combinedSerial);
            d.setCategory(it.getCategory());
            d.setCategoryLabel(categoryLabel(it.getCategory()));
            d.setType(it.getType());
            d.setTypeLabel(typeLabel(it.getType()));
            d.setStatus(it.getStatus()); // add status to dto
            d.setUserId(it.getUserId());
            d.setUsername(it.getUserId() != null ? usernameMap.get(it.getUserId()) : null);
            d.setUpdatedAt(it.getUpdatedAt());
            return d;
        }).toList();
    }

    private String categoryLabel(Integer v) {
        if (v == null) return "";
        return switch (v) {
            case 1 -> "Hóa đơn giá trị gia tăng";
            case 2 -> "Hóa đơn bán hàng";
            default -> "";
        };
    }
    private String typeLabel(Integer v) {
        if (v == null) return "";
        return switch (v) {
            case 1 -> "Một thuế suất";
            case 2 -> "Nhiều thuế suất";
            default -> "";
        };
    }

    private Map<String, Object> empty(int page, int size) {
        Map<String, Object> res = new HashMap<>();
        res.put("items", List.of());
        res.put("total", 0);
        res.put("per_page", size);
        res.put("current_page", page);
        res.put("last_page", 1);
        return res;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @GetMapping(value = "/{id}/download-xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<?> xmlDownload(@PathVariable Long id) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (!Objects.equals(e.getCompanyId(), user.getCompanyId())) return ResponseEntity.status(403).build();

        CompanyEntity company = null; CompanyBankEntity bank = null; LegalRepresentativeEntity rep = null;
        if (e.getCompanyId() != null) {
            company = companyRepository.findById(e.getCompanyId()).orElse(null);
            if (company != null) {
                java.util.List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
                if (banks != null && !banks.isEmpty()) bank = banks.get(0);
            }
            rep = legalRepresentativeRepository.findByCompanyId(e.getCompanyId()).orElse(null);
        }
        String sampleXml = vn.hoadon.util.SampleInvoiceXmlBuilder.build(user, e, company, bank, rep);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("invoice-" + id + ".xml").build());
        return new ResponseEntity<>(sampleXml, headers, HttpStatus.OK);
    }

    @GetMapping(value = "/{id}/download-pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<?> pdfDownload(@PathVariable Long id) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) return ResponseEntity.status(403).build();
        Optional<FormInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        FormInvoiceEntity e = opt.get();
        if (!Objects.equals(e.getCompanyId(), user.getCompanyId())) return ResponseEntity.status(403).build();

        String xsltPublicPath = e.getFile();
        if (!StringUtils.hasText(xsltPublicPath)) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN).body("Không tìm thấy tệp XSLT của mẫu");
        }
        Path xsltFsPath = toFilesystemPath(xsltPublicPath);
        if (xsltFsPath == null || !Files.exists(xsltFsPath)) {
            return ResponseEntity.status(404).contentType(MediaType.TEXT_PLAIN).body("Không tồn tại tệp XSLT trên hệ thống");
        }

        CompanyEntity company = null; CompanyBankEntity bank = null; LegalRepresentativeEntity rep = null;
        if (e.getCompanyId() != null) {
            company = companyRepository.findById(e.getCompanyId()).orElse(null);
            if (company != null) {
                java.util.List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
                if (banks != null && !banks.isEmpty()) bank = banks.get(0);
            }
            rep = legalRepresentativeRepository.findByCompanyId(e.getCompanyId()).orElse(null);
        }
        String sampleXml = vn.hoadon.util.SampleInvoiceXmlBuilder.build(user, e, company, bank, rep);

        try {
            TransformerFactory factory = TransformerFactory.newInstance();
            try { factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true); } catch (Exception ignored) {}
            try { factory.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "file"); } catch (Exception ignored) {}

            StreamSource xsltSource = new StreamSource(xsltFsPath.toFile());
            Transformer transformer = factory.newTransformer(xsltSource);

            StreamSource xmlSource = new StreamSource(new StringReader(sampleXml));
            StringWriter outWriter = new StringWriter();
            StreamResult result = new StreamResult(outWriter);
            transformer.transform(xmlSource, result);
            String html = outWriter.toString();

            // Resolve named HTML entities to avoid XML parser errors in the renderer
            html = resolveNamedHtmlEntities(html);

            // Normalize to XHTML-ish by self-closing void tags like <meta>, <link>, <img>, etc.
            html = normalizeToXhtml(html);

            // Convert HTML to PDF
            ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            String baseUri = xsltFsPath.getParent() != null ? xsltFsPath.getParent().toUri().toString() : null;
            builder.useFastMode();
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

    // Minimal resolver to replace common named HTML entities with Unicode characters
    private String resolveNamedHtmlEntities(String html) {
        if (html == null || html.isEmpty()) return html;
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("&([a-zA-Z][a-zA-Z0-9]+);");
        java.util.regex.Matcher m = p.matcher(html);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String name = m.group(1);
            if ("amp".equals(name) || "lt".equals(name) || "gt".equals(name) || "quot".equals(name) || "apos".equals(name)) {
                m.appendReplacement(sb, m.group(0));
                continue;
            }
            String repl = ENTITY_MAP.get(name);
            if (repl == null) repl = " ";
            m.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private static final java.util.Map<String, String> ENTITY_MAP = new java.util.HashMap<>();
    static {
        ENTITY_MAP.put("nbsp", "\u00A0");
        ENTITY_MAP.put("aacute", "\u00E1");
        ENTITY_MAP.put("eacute", "\u00E9");
        ENTITY_MAP.put("iacute", "\u00ED");
        ENTITY_MAP.put("oacute", "\u00F3");
        ENTITY_MAP.put("uacute", "\u00FA");
        ENTITY_MAP.put("yacute", "\u00FD");
        ENTITY_MAP.put("agrave", "\u00E0");
        ENTITY_MAP.put("ograve", "\u00F2");
        ENTITY_MAP.put("ntilde", "\u00F1");
        ENTITY_MAP.put("ocirc", "\u00F4");
        ENTITY_MAP.put("ouml", "\u00F6");
        ENTITY_MAP.put("uuml", "\u00FC");
        ENTITY_MAP.put("ccedil", "\u00E7");
        ENTITY_MAP.put("quot", "\"");
        ENTITY_MAP.put("apos", "'");
        ENTITY_MAP.put("lt", "<");
        ENTITY_MAP.put("gt", ">");
    }
}
