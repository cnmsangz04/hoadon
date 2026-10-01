package vn.hoadon.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.entity.*;
import vn.hoadon.repositories.*;
import vn.hoadon.services.ErrorMessageService;
import vn.hoadon.services.RegisterInvoiceService;
import vn.hoadon.util.InvoiceXmlBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping({"/v1/auth/pasigner", "/v1/auth"})
public class PasignerHashController {
    private static final ObjectMapper JSON = new ObjectMapper();

    private final HashInvoiceRepository hashInvoiceRepository;
    private final InvoiceRepository invoiceRepository;
    private final FormInvoiceRepository formInvoiceRepository;
    private final CompanyRepository companyRepository;
    private final CompanyBankRepository companyBankRepository;
    private final SignatureVatRepository signatureVatRepository;
    private final RegisterInvoiceRepository registerInvoiceRepository;
    private final RegisterInvoiceService registerInvoiceService;
    private final ErrorMessageRepository errorMessageRepository;
    private final ErrorMessageService errorMessageService;

    public PasignerHashController(HashInvoiceRepository hashInvoiceRepository,
                                  InvoiceRepository invoiceRepository,
                                  FormInvoiceRepository formInvoiceRepository,
                                  CompanyRepository companyRepository,
                                  CompanyBankRepository companyBankRepository,
                                  SignatureVatRepository signatureVatRepository,
                                  RegisterInvoiceRepository registerInvoiceRepository,
                                  RegisterInvoiceService registerInvoiceService,
                                  ErrorMessageRepository errorMessageRepository,
                                  ErrorMessageService errorMessageService) {
        this.hashInvoiceRepository = hashInvoiceRepository;
        this.invoiceRepository = invoiceRepository;
        this.formInvoiceRepository = formInvoiceRepository;
        this.companyRepository = companyRepository;
        this.companyBankRepository = companyBankRepository;
        this.signatureVatRepository = signatureVatRepository;
        this.registerInvoiceRepository = registerInvoiceRepository;
        this.registerInvoiceService = registerInvoiceService;
        this.errorMessageRepository = errorMessageRepository;
        this.errorMessageService = errorMessageService;
    }

    @PostMapping({"/getHashV2", "/get-hash", "/getHash"})
    public ResponseEntity<?> getHashV2(@RequestBody(required = false) String body) {
        JsonNode req = parseBody(body);
        String hash = text(req, "hash");
        Optional<HashInvoiceEntity> opt = hashInvoiceRepository.findByHash(hash);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("status", 404, "message", "Hash code not found in server."));
        }

        HashInvoiceEntity item = opt.get();
        CompanyEntity company = companyRepository.findById(item.getCompanyId()).orElse(null);
        List<Map<String, Object>> data = new ArrayList<>();

        if ("DKSD".equals(item.getType())) {
            for (Long id : item.getIds()) {
                RegisterInvoiceEntity register = registerInvoiceRepository.findById(id).orElse(null);
                if (register == null || !Objects.equals(register.getCompanyId(), item.getCompanyId())) continue;
                data.add(hashInvoiceRow(
                        register.getId(),
                        registerInvoiceService.buildUnsignedXml(register),
                        register.getDeclarationCode() != null ? register.getDeclarationCode() : String.valueOf(register.getId()),
                        valueOrDefault(item.getSignBy(), "NNT")
                ));
            }
        } else if ("HDGTGT".equals(item.getType())) {
            CompanyBankEntity bank = resolveCompanyBank(company);
            for (Long id : item.getIds()) {
                InvoiceEntity invoice = invoiceRepository.findById(id).orElse(null);
                if (invoice == null || invoice.getCompanyId() == null || !Objects.equals(invoice.getCompanyId().longValue(), item.getCompanyId())) continue;
                FormInvoiceEntity form = invoice.getFormId() != null ? formInvoiceRepository.findById(invoice.getFormId().longValue()).orElse(null) : null;
                data.add(hashInvoiceRow(
                        invoice.getId(),
                        normalizeInvoiceXml(InvoiceXmlBuilder.build(invoice, form, company, bank)),
                        invoice.getIdAttr() != null ? invoice.getIdAttr() : String.valueOf(invoice.getId()),
                        valueOrDefault(item.getSignBy(), "NBan")
                ));
            }
        } else if ("TBSS".equals(item.getType())) {
            for (Long id : item.getIds()) {
                ErrorMessageEntity message = errorMessageRepository.findById(id).orElse(null);
                if (message == null || !Objects.equals(message.getCompanyId(), item.getCompanyId())) continue;
                data.add(hashInvoiceRow(
                        message.getId(),
                        errorMessageService.buildUnsignedXml(message),
                        message.getIdAttr() != null ? message.getIdAttr() : String.valueOf(message.getId()),
                        valueOrDefault(item.getSignBy(), "NNT")
                ));
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("taxCode", company != null ? company.getTaxcode() : "");
        resp.put("status", 200);
        resp.put("invoices", data);
        return ResponseEntity.ok(resp);
    }

    @PostMapping({"/updateHashV2", "/update-hash", "/updateHash"})
    @Transactional
    public ResponseEntity<?> updateHashV2(@RequestBody(required = false) String body) {
        JsonNode req = parseBody(body);
        String hash = text(req, "hash");
        Optional<HashInvoiceEntity> opt = hashInvoiceRepository.findByHash(hash);
        if (opt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("status", 404, "message", "Hash code not found in server"));
        }

        HashInvoiceEntity item = opt.get();
        int signed = 0;
        for (SignedXmlEntry entry : signedXmlEntries(req.path("invs"))) {
            Long id = entry.id();
            String signedXml = entry.xml();
            if (id == null || signedXml == null || signedXml.isBlank() || !item.getIds().contains(id)) continue;

            if ("DKSD".equals(item.getType())) {
                RegisterInvoiceEntity register = registerInvoiceRepository.findById(id).orElse(null);
                if (register == null || !Objects.equals(register.getCompanyId(), item.getCompanyId())) continue;
                if (!isSignedRegisterXml(signedXml)) continue;
                String cert = req.has("certificate") ? req.get("certificate").toString() : resolveCompanyName(item.getCompanyId());
                if (registerInvoiceService.attachSignedXml(id, signedXml, cert).isPresent()) signed++;
            } else if ("TBSS".equals(item.getType())) {
                ErrorMessageEntity message = errorMessageRepository.findById(id).orElse(null);
                if (message == null || !Objects.equals(message.getCompanyId(), item.getCompanyId())) continue;
                if (!isSignedErrorMessageXml(signedXml)) continue;
                String cert = req.has("certificate") ? req.get("certificate").toString() : resolveCompanyName(item.getCompanyId());
                if (errorMessageService.attachSignedXml(id, signedXml, cert).isPresent()) signed++;
            } else if ("HDGTGT".equals(item.getType())) {
                InvoiceEntity invoice = invoiceRepository.findById(id).orElse(null);
                if (invoice == null || invoice.getCompanyId() == null || !Objects.equals(invoice.getCompanyId().longValue(), item.getCompanyId())) continue;
                if (invoice.getStatus() != null && invoice.getStatus() != 0) continue;
                if (!isSignedInvoiceXml(signedXml)) continue;
                SignatureVatEntity sig = signatureVatRepository
                        .findTopByInvoiceIdAndCompanyIdOrderByIdDesc(id.intValue(), item.getCompanyId().intValue())
                        .orElseGet(SignatureVatEntity::new);
                sig.setCompanyId(item.getCompanyId().intValue());
                sig.setInvoiceId(id.intValue());
                sig.setXml(normalizeInvoiceXml(signedXml));
                if (sig.getCreatedAt() == null) sig.setCreatedAt(LocalDateTime.now());
                sig.setUpdatedAt(LocalDateTime.now());
                signatureVatRepository.save(sig);

                invoice.setStatus((short) 1);
                invoice.setUpdatedAt(LocalDateTime.now());
                invoiceRepository.save(invoice);
                signed++;
            }
        }

        if (signed == 0) {
            return ResponseEntity.status(500).body(Map.of("status", 500, "message", "Error"));
        }
        hashInvoiceRepository.delete(item);
        return ResponseEntity.ok(Map.of("status", 200, "message", "Successful!", "numOfSigned", signed));
    }

    private JsonNode parseBody(String body) {
        String raw = body != null ? body.trim() : "";
        if (raw.isBlank()) return JSON.createObjectNode();

        try {
            return JSON.readTree(raw);
        } catch (Exception ignored) {}

        try {
            String decoded = URLDecoder.decode(raw, StandardCharsets.UTF_8);
            return JSON.readTree(decoded);
        } catch (Exception ignored) {}

        return parseFormBody(raw);
    }

    private JsonNode parseFormBody(String body) {
        ObjectNode root = JSON.createObjectNode();
        for (String pair : body.split("&")) {
            int idx = pair.indexOf('=');
            if (idx < 0) continue;
            try {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                if (value.startsWith("{") || value.startsWith("[")) {
                    root.set(key, JSON.readTree(value));
                } else {
                    root.put(key, value);
                }
            } catch (Exception ignored) {}
        }
        return root;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node != null ? node.get(field) : null;
        return value == null || value.isNull() ? null : value.asText();
    }

    private Map<String, Object> hashInvoiceRow(Long id, String xml, String invId, String signBy) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("xml", xml != null ? xml : "");
        row.put("invId", invId != null ? invId : String.valueOf(id));
        row.put("signBy", signBy);
        return row;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null && !value.isBlank()) return value;
        }
        return null;
    }

    private List<SignedXmlEntry> signedXmlEntries(JsonNode invs) {
        List<SignedXmlEntry> entries = new ArrayList<>();
        if (invs == null || invs.isMissingNode() || invs.isNull()) return entries;

        if (invs.isTextual()) {
            try {
                return signedXmlEntries(JSON.readTree(invs.asText()));
            } catch (Exception ignored) {
                return entries;
            }
        }

        if (invs.isArray()) {
            for (JsonNode row : invs) {
                if (!row.isObject()) continue;
                Long id = row.path("id").isNumber() ? row.path("id").asLong() : parseLong(firstText(row, "id", "invoiceId", "invId"));
                String xml = firstText(row, "signedXml", "xmlSigned", "signed_xml", "xml");
                entries.add(new SignedXmlEntry(id, xml));
            }
            return entries;
        }

        if (invs.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = invs.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                Long id = parseLong(field.getKey());
                JsonNode value = field.getValue();
                String xml = value.isObject()
                        ? firstText(value, "signedXml", "xmlSigned", "signed_xml", "xml")
                        : value.asText(null);
                entries.add(new SignedXmlEntry(id, xml));
            }
        }
        return entries;
    }

    private Long parseLong(String value) {
        try {
            return value == null || value.isBlank() ? null : Long.parseLong(value);
        } catch (Exception e) {
            return null;
        }
    }

    private CompanyBankEntity resolveCompanyBank(CompanyEntity company) {
        if (company == null) return null;
        List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
        return banks != null && !banks.isEmpty() ? banks.get(0) : null;
    }

    private String normalizeInvoiceXml(String xml) {
        if (xml == null || xml.isBlank()) return xml;
        return xml.replaceFirst("(?is)<DSCKS>\\s*<NBan\\s*/>\\s*<NMua\\s*/>\\s*<CCKSKhac\\s*/>\\s*</DSCKS>", "<DSCKS><NBan/><NMua/><CQT/><CCKSKhac/></DSCKS>");
    }

    private boolean isSignedInvoiceXml(String xml) {
        return xml != null && xml.contains("<HDon") && xml.contains("<Signature");
    }

    private boolean isSignedRegisterXml(String xml) {
        return xml != null && xml.contains("<TKhai") && xml.contains("<Signature");
    }

    private boolean isSignedErrorMessageXml(String xml) {
        return xml != null && xml.contains("<TBao") && xml.contains("<Signature");
    }

    private String resolveCompanyName(Long companyId) {
        if (companyId == null) return "";
        return companyRepository.findById(companyId).map(c -> c.getName() != null ? c.getName() : "").orElse("");
    }

    private String valueOrDefault(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }

    private record SignedXmlEntry(Long id, String xml) {}
}
