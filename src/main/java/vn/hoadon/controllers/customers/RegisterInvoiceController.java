package vn.hoadon.controllers.customers;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.dto.registerinvoice.RegisterInvoicePrefillDto;
import vn.hoadon.dto.registerinvoice.RegisterInvoiceUpsertRequest;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.RegisterInvoiceEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.services.RegisterInvoiceService;
import vn.hoadon.services.HistoryService;
import vn.hoadon.dto.history.HistoryDto;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

// JSON
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;

@RestController
@RequestMapping("/v1/register-invoices")
public class RegisterInvoiceController extends BaseController {
    private final RegisterInvoiceService service;
    private final CompanyRepository companyRepository;
    // Replace repository with service per 3-layer architecture
    @Autowired(required = false)
    private HistoryService historyService;
    private final ObjectMapper mapper = new ObjectMapper();

    @PersistenceContext
    private EntityManager entityManager;

    public RegisterInvoiceController(RegisterInvoiceService service, CompanyRepository companyRepository) {
        this.service = service;
        this.companyRepository = companyRepository;
    }

    @GetMapping("/{id}")
    public ResponseEntity<RegisterInvoiceEntity> getById(@PathVariable Long id) {
        permission("register-invoice-list");
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<RegisterInvoiceEntity>> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long userId) {
        permission("register-invoice-list");
        if (companyId != null) {
            return ResponseEntity.ok(service.findByCompany(companyId));
        }
        if (userId != null) {
            return ResponseEntity.ok(service.findByUser(userId));
        }
        // Dự phòng: dùng ngữ cảnh người dùng hiện tại
        UserEntity user = currentUser();
        if (user != null) {
            // prefer company scope if available
            if (user.getCompanyId() != null) {
                return ResponseEntity.ok(service.findByCompany(user.getCompanyId()));
            }
            return ResponseEntity.ok(service.findByUser(user.getId()));
        }
        // default: return empty to avoid exposing all
        return ResponseEntity.ok(List.of());
    }

    // Prefill data for create.vue - auto-detect companyId from authenticated user
    @GetMapping("/prefill")
    public ResponseEntity<Map<String, Object>> prefill(
            @RequestParam(required = false) Integer declarationType,
            @RequestParam(required = false) LocalDate effectiveDate) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<RegisterInvoicePrefillDto> dtoOpt = service.prefill(user.getCompanyId(), declarationType, effectiveDate);
        if (dtoOpt.isEmpty()) return ResponseEntity.notFound().build();
        RegisterInvoicePrefillDto dto = dtoOpt.get();
        Map<String, Object> res = new HashMap<>();
        // Flat fields (backward-compatible)
        res.put("declarationType", dto.getDeclarationType());
        res.put("effectiveDate", dto.getEffectiveDate());
        res.put("companyName", dto.getCompanyName());
        res.put("taxCode", dto.getTaxCode());
        res.put("taxAuthorityName", dto.getTaxAuthorityName());
        res.put("contactAddress", dto.getContactAddress());
        res.put("contactEmail", dto.getContactEmail());
        res.put("legalFullname", dto.getLegalFullname());
        res.put("legalPhone", dto.getLegalPhone());
        res.put("legalCitizenId", dto.getLegalCitizenId());
        res.put("legalPassportNo", dto.getLegalPassportNo());
        res.put("legalDateOfBirth", dto.getLegalDateOfBirth());
        res.put("legalGender", dto.getLegalGender());
        // Nested company and legal representative info
        Map<String, Object> company = new HashMap<>();
        company.put("name", dto.getCompanyName());
        company.put("email", dto.getContactEmail());
        company.put("address", dto.getContactAddress());
        company.put("taxCode", dto.getTaxCode());
        res.put("company", company);
        Map<String, Object> legal = new HashMap<>();
        legal.put("fullname", dto.getLegalFullname());
        legal.put("phone", dto.getLegalPhone());
        legal.put("citizen_id", dto.getLegalCitizenId());
        legal.put("passport_no", dto.getLegalPassportNo());
        legal.put("date_of_birth", dto.getLegalDateOfBirth());
        legal.put("gender", dto.getLegalGender());
        res.put("legalRepresentative", legal);
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<RegisterInvoiceEntity> create(@RequestBody RegisterInvoiceUpsertRequest req) {
        permission("register-invoice-save");
        UserEntity user = currentUser();
        if (user == null || user.getId() == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().build();
        }
        RegisterInvoiceEntity entity = new RegisterInvoiceEntity();
        entity.setCompanyId(user.getCompanyId());
        entity.setUserId(user.getId());
        entity.setDeclarationCode(null);
        entity.setDeclarationType(req.getDeclarationType() != null ? req.getDeclarationType() : 1);
        entity.setFormPattern(req.getFormPattern() != null ? req.getFormPattern() : "01/ĐKTĐ-HĐĐT");
        entity.setDeclarationDate(parseDate(req.getDeclarationDate(), LocalDate.now()));
        entity.setCreatePlace(req.getCreatePlace());
        // Ngừng lấy effectiveDate từ request; ban đầu để null
        entity.setEffectiveDate(null);
        // List fields
        entity.setInvoiceForms(toStringList(req.getInvoiceForms()));
        entity.setInvoiceTypes(toStringList(req.getInvoiceTypes()));
        entity.setTransferMethods(toStringList(req.getTransferMethods()));
        entity.setSendMethods(toStringList(req.getSendMethods()));
        entity.setDigitalCertificates(toStringList(req.getDigitalCertificates()));
        entity.setSolutionProviders(toStringList(req.getSolutionProviders()));
        entity.setTransmitProviders(toStringList(req.getTransmitProviders()));
        entity.setStatus(req.getStatus() != null ? req.getStatus() : 0);
        RegisterInvoiceEntity created = service.create(entity);
        return ResponseEntity.created(URI.create("/v1/register-invoices/" + created.getId())).body(created);
    }

    @PutMapping("/{id}/get")
    public ResponseEntity<RegisterInvoiceEntity> update(@PathVariable Long id, @RequestBody RegisterInvoiceUpsertRequest req) {
        permission("register-invoice-save");
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.badRequest().build();
        }
        Optional<RegisterInvoiceEntity> existingOpt = service.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        RegisterInvoiceEntity existing = existingOpt.get();
        if (!user.getCompanyId().equals(existing.getCompanyId())) {
            return ResponseEntity.status(403).build();
        }
        RegisterInvoiceEntity patch = new RegisterInvoiceEntity();
        patch.setCompanyId(existing.getCompanyId());
        patch.setUserId(existing.getUserId() != null ? existing.getUserId() : user.getId());
        patch.setDeclarationCode(existing.getDeclarationCode());
        patch.setDeclarationType(req.getDeclarationType() != null ? req.getDeclarationType() : existing.getDeclarationType());
        patch.setFormPattern(req.getFormPattern() != null ? req.getFormPattern() : existing.getFormPattern());
        patch.setDeclarationDate(parseDate(req.getDeclarationDate(), existing.getDeclarationDate()));
        // company_name, tax_code derived; ignore request values
        patch.setCreatePlace(req.getCreatePlace() != null ? req.getCreatePlace() : existing.getCreatePlace());
        // Ignore effectiveDate from request; preserve existing until status logic sets it
        patch.setEffectiveDate(existing.getEffectiveDate());
        // List fields
        patch.setInvoiceForms(req.getInvoiceForms() != null ? toStringList(req.getInvoiceForms()) : existing.getInvoiceForms());
        patch.setInvoiceTypes(req.getInvoiceTypes() != null ? toStringList(req.getInvoiceTypes()) : existing.getInvoiceTypes());
        patch.setTransferMethods(req.getTransferMethods() != null ? toStringList(req.getTransferMethods()) : existing.getTransferMethods());
        patch.setSendMethods(req.getSendMethods() != null ? toStringList(req.getSendMethods()) : existing.getSendMethods());
        patch.setDigitalCertificates(req.getDigitalCertificates() != null ? toStringList(req.getDigitalCertificates()) : existing.getDigitalCertificates());
        patch.setSolutionProviders(req.getSolutionProviders() != null ? toStringList(req.getSolutionProviders()) : existing.getSolutionProviders());
        patch.setTransmitProviders(req.getTransmitProviders() != null ? toStringList(req.getTransmitProviders()) : existing.getTransmitProviders());
        patch.setSignedXml(existing.getSignedXml());
        patch.setSignatureInfo(existing.getSignatureInfo());
        patch.setStatus(req.getStatus() != null ? req.getStatus() : existing.getStatus());
        Optional<RegisterInvoiceEntity> updated = service.update(id, patch);
        return updated.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/send")
    public ResponseEntity<Void> sendToCQT(@PathVariable Long id) {
        permission("register-invoice-send");
        UserEntity user = currentUser();
        if (user == null) return ResponseEntity.status(403).build();
        Optional<RegisterInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        RegisterInvoiceEntity entity = opt.get();
        // permission: non-root must match company
        Integer role = user.getRole();
        boolean isRoot = role != null && role == 0;
        if (!isRoot) {
            if (user.getCompanyId() == null || !user.getCompanyId().equals(entity.getCompanyId())) {
                return ResponseEntity.status(403).build();
            }
        }
        // Must be signed before sending
        if (entity.getSignedXml() == null || entity.getSignedXml().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        // 1) Insert history row
        insertHistoryRow(user.getCompanyId(), user.getId(), "register_invoices", entity.getId(),
                "Gửi Đăng ký/Thay đổi thông tin sử dụng hóa đơn điện tử", "Mã thông điệp 100", 1, 1, 100, "");
        // 2. Cập nhật status = 2 (Đã ký) theo yêu cầu khi gửi thành công
        // Preserve all existing fields to avoid accidental data loss if update is a full overwrite
        RegisterInvoiceEntity patch = cloneEntityPreserveAll(entity);
        // Mutating fields only
        patch.setStatus(2);
        patch.setResponseReceiveFile(null);
        patch.setResponseAcceptFile(null);
        service.update(id, patch);
        // 3) Simulate async tax responses
        simulateAsyncTaxResponses(entity);
        return ResponseEntity.accepted().build();
    }

    // Hàm hỗ trợ lưu lịch sử bằng JPA thay cho SQL thô
    private void insertHistoryRow(Long companyId, Long userId, String tableName, Long tableId,
                                  String title, String description, Integer showNotify, Integer status,
                                  Integer type, String xmlData) {
        try {
            if (historyService == null) return;
            HistoryDto dto = new HistoryDto();
            dto.setCompanyId(companyId);
            dto.setUserId(userId);
            dto.setTableName(tableName);
            dto.setTableId(tableId);
            dto.setTitle(title);
            dto.setDescription(description);
            dto.setShowNotify(showNotify);
            dto.setStatus(status);
            dto.setType(type);
            dto.setXmlData(xmlData);
            historyService.save(dto);
        } catch (Exception ignored) {
        }
    }

    // Hàm hỗ trợ clone entity và giữ toàn bộ trường để ghi đè an toàn
    private RegisterInvoiceEntity cloneEntityPreserveAll(RegisterInvoiceEntity src) {
        if (src == null) return new RegisterInvoiceEntity();
        RegisterInvoiceEntity e = new RegisterInvoiceEntity();
        // Identity and required fields
        e.setCompanyId(src.getCompanyId());
        e.setUserId(src.getUserId());
        e.setDeclarationCode(src.getDeclarationCode());
        e.setDeclarationType(src.getDeclarationType());
        e.setFormPattern(src.getFormPattern());
        e.setDeclarationDate(src.getDeclarationDate());
        e.setCreatePlace(src.getCreatePlace());
        e.setEffectiveDate(src.getEffectiveDate());
        // JSON/text fields
        e.setInvoiceForms(src.getInvoiceForms());
        e.setInvoiceTypes(src.getInvoiceTypes());
        e.setTransferMethods(src.getTransferMethods());
        e.setSendMethods(src.getSendMethods());
        e.setDigitalCertificates(src.getDigitalCertificates());
        e.setSolutionProviders(src.getSolutionProviders());
        e.setTransmitProviders(src.getTransmitProviders());
        // Signing and responses
        e.setSignedXml(src.getSignedXml());
        e.setSignatureInfo(src.getSignatureInfo());
        // Preserve signature date
        try { e.setSignDate(src.getSignDate()); } catch (NoSuchMethodError | Exception ignored) {}
        e.setResponseReceiveFile(src.getResponseReceiveFile());
        e.setResponseAcceptFile(src.getResponseAcceptFile());
        // Status and any other fields
        e.setStatus(src.getStatus());
        return e;
    }

    private void simulateAsyncTaxResponses(RegisterInvoiceEntity entity) {
        // Clone basic info needed
        final Long id = entity.getId();
        final Long companyId = entity.getCompanyId();
        // Simulate 102 after ~2 seconds
        new Thread(() -> {
            try { Thread.sleep(2000L); } catch (InterruptedException ignored) {}
            boolean acceptedReceive = decideByCompany(companyId);
            String xml = buildResponseXml(102, acceptedReceive ? 1 : 0);
            RegisterInvoiceEntity patch = cloneEntityPreserveAll(entity);
            patch.setStatus(acceptedReceive ? 4 : 3);
            patch.setResponseReceiveFile(xml);
            service.update(id, patch);
            // Insert history row for message 102
            String desc102 = acceptedReceive ? "Mã thông điệp 102 đã tiếp nhận" : "Mã thông điệp 102 không tiếp nhận";
            insertHistoryRow(companyId, 0L, "register_invoices", id,
                    "Thông điệp 102 tiếp nhận thông tin tờ khai hóa đơn điện tử", desc102, 1, 1, 102, xml);
        }).start();
        // Simulate 103 after ~5 seconds
        new Thread(() -> {
            try { Thread.sleep(5000L); } catch (InterruptedException ignored) {}
            boolean accepted = decideByCompany(companyId);
            String xml = buildResponseXml(103, accepted ? 1 : 0);
            RegisterInvoiceEntity patch = cloneEntityPreserveAll(entity);
            patch.setStatus(accepted ? 6 : 5);
            patch.setResponseAcceptFile(xml);
            // Khi status = 6 (đã chấp nhận), set effectiveDate là thời gian hiện tại
            if (accepted) {
                try { patch.setEffectiveDate(java.time.LocalDateTime.now()); } catch (Exception ignored) {}
            }
            service.update(id, patch);
            // Insert history row for message 103
            String desc103 = accepted ? "Mã thông điệp 103 đã chấp nhận" : "Mã thông điệp 103 không chấp nhận";
            insertHistoryRow(companyId, 0L, "register_invoices", id,
                    "Thông điệp 103 tiếp nhận thông tin tờ khai hóa đơn điện tử", desc103, 1, 1, 103, xml);
        }).start();
    }

    private boolean decideByCompany(Long companyId) {
        if (companyId == null) return true;
        // simple deterministic pseudo-random decision based on companyId
        return Math.abs(Objects.hash(companyId)) % 2 == 0;
    }

    private String buildResponseXml(int mltDiep, int flag) {
        // TTChung->MLTDiep and DLieu->TBao->DLTBao->THop or TTXNCQT per requirement
        StringBuilder sb = new StringBuilder();
        sb.append("<TBaoCQT>");
        sb.append("<TTChung><MLTDiep>").append(mltDiep).append("</MLTDiep></TTChung>");
        sb.append("<DLieu><TBao><DLTBao>");
        if (mltDiep == 102) {
            sb.append("<THop>").append(flag).append("</THop>");
        } else {
            sb.append("<TTXNCQT>").append(flag).append("</TTXNCQT>");
        }
        sb.append("</DLTBao></TBao></DLieu>");
        sb.append("</TBaoCQT>");
        return sb.toString();
    }

    @GetMapping("/list")
    public Map<String, Object> listPaged(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Integer declarationType
    ) {
        permission("register-invoice-list");
        UserEntity user = currentUser();
        Long actorCompanyId = user != null ? user.getCompanyId() : null;
        Integer actorRole = user != null ? user.getRole() : null;
        boolean isRoot = actorRole != null && actorRole == 0;

        // Nếu không phải root, giới hạn theo công ty của người dùng
        if (!isRoot) {
            companyId = actorCompanyId;
        }

        // Spring pages are 0-based; apply default sort desc by createdAt
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RegisterInvoiceEntity> p;

        // Nếu là root và không truyền companyId, cho phép xem toàn bộ
        if (isRoot && companyId == null) {
            p = service.pageAll(pageable);
            // Optional filter by status
            if (status != null) {
                List<RegisterInvoiceEntity> filteredByStatus = p.getContent().stream()
                        .filter(e -> status.equals(e.getStatus()))
                        .toList();
                p = new org.springframework.data.domain.PageImpl<>(filteredByStatus, pageable, filteredByStatus.size());
            }
            // Keyword filter by company name/tax code via companies table
            if (keyword != null && !keyword.isBlank()) {
                Set<Long> ids = p.getContent().stream().map(RegisterInvoiceEntity::getCompanyId).collect(Collectors.toSet());
                Map<Long, CompanyEntity> map = new HashMap<>();
                if (!ids.isEmpty()) {
                    for (CompanyEntity c : companyRepository.findAllById(ids)) {
                        map.put(c.getId(), c);
                    }
                }
                String kw = keyword.toLowerCase();
                List<RegisterInvoiceEntity> filtered = p.getContent().stream()
                        .filter(e -> {
                            CompanyEntity c = map.get(e.getCompanyId());
                            String name = c != null && c.getName() != null ? c.getName().toLowerCase() : "";
                            String tax = c != null && c.getTaxcode() != null ? c.getTaxcode().toLowerCase() : "";
                            return name.contains(kw) || tax.contains(kw);
                        })
                        .toList();
                p = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
            }
            // Optional filter by declarationType for global branch
            if (declarationType != null) {
                List<RegisterInvoiceEntity> filteredByDecl = p.getContent().stream()
                        .filter(e -> declarationType.equals(e.getDeclarationType()))
                        .toList();
                p = new org.springframework.data.domain.PageImpl<>(filteredByDecl, pageable, filteredByDecl.size());
            }
            // Áp dụng bộ lọc ngày cho cả nhánh toàn cục
            try {
                java.time.LocalDate from = (dateFrom != null && !dateFrom.isBlank()) ? java.time.LocalDate.parse(dateFrom) : null;
                java.time.LocalDate to = (dateTo != null && !dateTo.isBlank()) ? java.time.LocalDate.parse(dateTo) : null;
                if (from != null || to != null) {
                    List<RegisterInvoiceEntity> filtered = p.getContent().stream()
                            .filter(e -> {
                                java.time.LocalDate d = e.getDeclarationDate();
                                if (d == null) return false;
                                if (from != null && to == null) {
                                    return d.equals(from);
                                }
                                if (from == null && to != null) {
                                    return d.equals(to);
                                }
                                boolean geFrom = !d.isBefore(from);
                                boolean leTo = !d.isAfter(to);
                                return geFrom && leTo;
                            })
                            .toList();
                    p = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
                }
            } catch (Exception ignored) {}
            return toPaginationResponse(p);
        }

        // Nếu companyId vẫn null (không có phạm vi), trả về phân trang rỗng
        if (companyId == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("data", List.of());
            empty.put("total", 0);
            empty.put("per_page", size);
            empty.put("current_page", page);
            empty.put("last_page", 1);
            empty.put("from", 0);
            empty.put("to", 0);
            empty.put("prev_page_url", null);
            empty.put("next_page_url", null);
            return empty;
        }

        // Company-scoped filters
        if (status != null) {
            p = service.pageByCompanyAndStatus(companyId, status, pageable);
        } else {
            p = service.pageByCompany(companyId, pageable);
        }

        // Optional keyword filter by company name/tax code via companies table (within current page)
        if (keyword != null && !keyword.isBlank()) {
            Set<Long> ids = p.getContent().stream().map(RegisterInvoiceEntity::getCompanyId).collect(Collectors.toSet());
            Map<Long, CompanyEntity> map = new HashMap<>();
            if (!ids.isEmpty()) {
                for (CompanyEntity c : companyRepository.findAllById(ids)) {
                    map.put(c.getId(), c);
                }
            }
            String kw = keyword.toLowerCase();
            List<RegisterInvoiceEntity> filtered = p.getContent().stream()
                    .filter(e -> {
                        CompanyEntity c = map.get(e.getCompanyId());
                        String name = c != null && c.getName() != null ? c.getName().toLowerCase() : "";
                        String tax = c != null && c.getTaxcode() != null ? c.getTaxcode().toLowerCase() : "";
                        return name.contains(kw) || tax.contains(kw);
                    })
                    .toList();
            p = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
        }

        // Optional filter by declarationType in company branch
        if (declarationType != null) {
            List<RegisterInvoiceEntity> filteredByDecl = p.getContent().stream()
                    .filter(e -> declarationType.equals(e.getDeclarationType()))
                    .toList();
            p = new org.springframework.data.domain.PageImpl<>(filteredByDecl, pageable, filteredByDecl.size());
        }

        // Optional date range filter on declarationDate; support single-bound filters
        try {
            java.time.LocalDate from = (dateFrom != null && !dateFrom.isBlank()) ? java.time.LocalDate.parse(dateFrom) : null;
            java.time.LocalDate to = (dateTo != null && !dateTo.isBlank()) ? java.time.LocalDate.parse(dateTo) : null;
            if (from != null || to != null) {
                List<RegisterInvoiceEntity> filtered = p.getContent().stream()
                        .filter(e -> {
                            java.time.LocalDate d = e.getDeclarationDate();
                            if (d == null) return false;
                            if (from != null && to == null) {
                                return d.equals(from);
                            }
                            if (from == null && to != null) {
                                return d.equals(to);
                            }
                            // Both bounds provided: inclusive range
                            boolean geFrom = !d.isBefore(from);
                            boolean leTo = !d.isAfter(to);
                            return geFrom && leTo;
                        })
                        .toList();
                p = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
            }
        } catch (Exception ignored) {}

        return toPaginationResponse(p);
    }

    private Map<String, Object> toPaginationResponse(Page<RegisterInvoiceEntity> p) {
        Map<String, Object> res = new HashMap<>();
        long total = p.getTotalElements();
        int size = p.getSize();
        int currentPage = p.getNumber() + 1; // bắt đầu từ 1 for frontend
        int lastPage = Math.max(1, p.getTotalPages());
        int numberOfElements = p.getNumberOfElements();
        long from = total == 0 ? 0 : ((long) (currentPage - 1) * size) + 1;
        long to = total == 0 ? 0 : (from + numberOfElements - 1);

        res.put("data", p.getContent());
        res.put("total", total);
        res.put("per_page", size);
        res.put("current_page", currentPage);
        res.put("last_page", lastPage);
        res.put("from", from);
        res.put("to", to);
        res.put("prev_page_url", currentPage > 1 ? currentPage - 1 : null);
        res.put("next_page_url", currentPage < lastPage ? currentPage + 1 : null);
        return res;
    }

    // --- Helpers for flexible JSON binding ---
    private LocalDate parseDate(String v, LocalDate def) {
        if (v == null || v.isBlank()) return def;
        try { return LocalDate.parse(v); } catch (Exception e) { return def; }
    }

    private List<String> toStringList(Object v) {
        if (v == null) return null;
        try {
            if (v instanceof List<?> list) {
                List<String> out = new ArrayList<>();
                for (Object o : list) {
                    if (o == null) {
                        out.add(null);
                    } else if (o instanceof String) {
                        out.add((String) o);
                    } else {
                        // Chuyển object thành chuỗi JSON thay vì dùng toString()
                        try {
                            out.add(mapper.writeValueAsString(o));
                        } catch (Exception e) {
                            out.add(String.valueOf(o));
                        }
                    }
                }
                return out;
            }
            if (v instanceof String s) {
                String trimmed = s.trim();
                if (trimmed.isEmpty()) return List.of();
                // Thử mảng JSON trước
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    ArrayNode arr = (ArrayNode) mapper.readTree(trimmed);
                    List<String> out = new ArrayList<>();
                    for (JsonNode node : arr) out.add(node.isNull() ? null : node.asText());
                    return out;
                }
                // Dự phòng: CSV
                String[] parts = trimmed.split(",");
                List<String> out = new ArrayList<>();
                for (String p : parts) {
                    if (p == null) continue;
                    String token = p.trim();
                    if (token.startsWith("\"") && token.endsWith("\"")) {
                        token = token.substring(1, token.length() - 1);
                      }
                    if (!token.isEmpty()) out.add(token);
                }
                return out;
            }
            // Tổng quát: serialize sang JSON rồi parse nếu là mảng
            JsonNode node = mapper.valueToTree(v);
            if (node != null && node.isArray()) {
                ArrayNode arr = (ArrayNode) node;
                List<String> out = new ArrayList<>();
                for (JsonNode i : arr) {
                    if (i.isNull()) {
                        out.add(null);
                    } else if (i.isTextual()) {
                        out.add(i.asText());
                    } else {
                        // Chuyển JsonNode về lại chuỗi JSON
                        out.add(mapper.writeValueAsString(i));
                    }
                }
                return out;
            }
            // Giá trị đơn - serialize sang JSON nếu là object
            try {
                return List.of(mapper.writeValueAsString(v));
            } catch (Exception e) {
                return List.of(String.valueOf(v));
            }
        } catch (Exception e) {
            // Dự phòng best-effort
            return List.of(String.valueOf(v));
        }
    }

    @GetMapping("/list-old")
    public Map<String, Object> listPagedOld(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Integer declarationType
    ) {
        UserEntity user = currentUser();
        Long actorCompanyId = user != null ? user.getCompanyId() : null;
        Integer actorRole = user != null ? user.getRole() : null;
        boolean isRoot = actorRole != null && actorRole == 0;

        // Nếu không phải root, giới hạn theo công ty của người dùng
        if (!isRoot) {
            companyId = actorCompanyId;
        }

        // Spring pages are 0-based; apply default sort desc by createdAt
        Pageable pageable = PageRequest.of(Math.max(0, page - 1), size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RegisterInvoiceEntity> p;

        // Nếu là root và không truyền companyId, cho phép xem toàn bộ
        if (isRoot && companyId == null) {
            p = service.pageAll(pageable);
            // Optional filter by status
            if (status != null) {
                List<RegisterInvoiceEntity> filteredByStatus = p.getContent().stream()
                        .filter(e -> status.equals(e.getStatus()))
                        .toList();
                p = new org.springframework.data.domain.PageImpl<>(filteredByStatus, pageable, filteredByStatus.size());
            }
            // Keyword filter by company name/tax code via companies table
            if (keyword != null && !keyword.isBlank()) {
                Set<Long> ids = p.getContent().stream().map(RegisterInvoiceEntity::getCompanyId).collect(Collectors.toSet());
                Map<Long, CompanyEntity> map = new HashMap<>();
                if (!ids.isEmpty()) {
                    for (CompanyEntity c : companyRepository.findAllById(ids)) {
                        map.put(c.getId(), c);
                    }
                }
                String kw = keyword.toLowerCase();
                List<RegisterInvoiceEntity> filtered = p.getContent().stream()
                        .filter(e -> {
                            CompanyEntity c = map.get(e.getCompanyId());
                            String name = c != null && c.getName() != null ? c.getName().toLowerCase() : "";
                            String tax = c != null && c.getTaxcode() != null ? c.getTaxcode().toLowerCase() : "";
                            return name.contains(kw) || tax.contains(kw);
                        })
                        .toList();
                p = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
            }
            // Optional filter by declarationType for global branch
            if (declarationType != null) {
                List<RegisterInvoiceEntity> filteredByDecl = p.getContent().stream()
                        .filter(e -> declarationType.equals(e.getDeclarationType()))
                        .toList();
                p = new org.springframework.data.domain.PageImpl<>(filteredByDecl, pageable, filteredByDecl.size());
            }
            // Áp dụng bộ lọc ngày cho cả nhánh toàn cục
            try {
                java.time.LocalDate from = (dateFrom != null && !dateFrom.isBlank()) ? java.time.LocalDate.parse(dateFrom) : null;
                java.time.LocalDate to = (dateTo != null && !dateTo.isBlank()) ? java.time.LocalDate.parse(dateTo) : null;
                if (from != null || to != null) {
                    List<RegisterInvoiceEntity> filtered = p.getContent().stream()
                            .filter(e -> {
                                java.time.LocalDate d = e.getDeclarationDate();
                                if (d == null) return false;
                                if (from != null && to == null) {
                                    return d.equals(from);
                                }
                                if (from == null && to != null) {
                                    return d.equals(to);
                                }
                                boolean geFrom = !d.isBefore(from);
                                boolean leTo = !d.isAfter(to);
                                return geFrom && leTo;
                            })
                            .toList();
                    p = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
                }
            } catch (Exception ignored) {}
            return toPaginationResponse(p);
        }

        // Nếu companyId vẫn null (không có phạm vi), trả về phân trang rỗng
        if (companyId == null) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("data", List.of());
            empty.put("total", 0);
            empty.put("per_page", size);
            empty.put("current_page", page);
            empty.put("last_page", 1);
            empty.put("from", 0);
            empty.put("to", 0);
            empty.put("prev_page_url", null);
            empty.put("next_page_url", null);
            return empty;
        }

        // Company-scoped filters
        if (status != null) {
            p = service.pageByCompanyAndStatus(companyId, status, pageable);
        } else {
            p = service.pageByCompany(companyId, pageable);
        }

        // Optional keyword filter by company name/tax code via companies table (within current page)
        if (keyword != null && !keyword.isBlank()) {
            Set<Long> ids = p.getContent().stream().map(RegisterInvoiceEntity::getCompanyId).collect(Collectors.toSet());
            Map<Long, CompanyEntity> map = new HashMap<>();
            if (!ids.isEmpty()) {
                for (CompanyEntity c : companyRepository.findAllById(ids)) {
                    map.put(c.getId(), c);
                }
            }
            String kw = keyword.toLowerCase();
            List<RegisterInvoiceEntity> filtered = p.getContent().stream()
                    .filter(e -> {
                        CompanyEntity c = map.get(e.getCompanyId());
                        String name = c != null && c.getName() != null ? c.getName().toLowerCase() : "";
                        String tax = c != null && c.getTaxcode() != null ? c.getTaxcode().toLowerCase() : "";
                        return name.contains(kw) || tax.contains(kw);
                    })
                    .toList();
            p = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
        }

        // Optional filter by declarationType in company branch
        if (declarationType != null) {
            List<RegisterInvoiceEntity> filteredByDecl = p.getContent().stream()
                    .filter(e -> declarationType.equals(e.getDeclarationType()))
                    .toList();
            p = new org.springframework.data.domain.PageImpl<>(filteredByDecl, pageable, filteredByDecl.size());
        }

        // Optional date range filter on declarationDate; support single-bound filters
        try {
            java.time.LocalDate from = (dateFrom != null && !dateFrom.isBlank()) ? java.time.LocalDate.parse(dateFrom) : null;
            java.time.LocalDate to = (dateTo != null && !dateTo.isBlank()) ? java.time.LocalDate.parse(dateTo) : null;
            if (from != null || to != null) {
                List<RegisterInvoiceEntity> filtered = p.getContent().stream()
                        .filter(e -> {
                            java.time.LocalDate d = e.getDeclarationDate();
                            if (d == null) return false;
                            if (from != null && to == null) {
                                return d.equals(from);
                            }
                            if (from == null && to != null) {
                                return d.equals(to);
                            }
                            // Both bounds provided: inclusive range
                            boolean geFrom = !d.isBefore(from);
                            boolean leTo = !d.isAfter(to);
                            return geFrom && leTo;
                        })
                        .toList();
                p = new org.springframework.data.domain.PageImpl<>(filtered, pageable, filtered.size());
            }
        } catch (Exception ignored) {}

        return toPaginationResponse(p);
    }

    private Map<String, Object> toPaginationResponseOld(Page<RegisterInvoiceEntity> p) {
        Map<String, Object> res = new HashMap<>();
        long total = p.getTotalElements();
        int size = p.getSize();
        int currentPage = p.getNumber() + 1; // bắt đầu từ 1 for frontend
        int lastPage = Math.max(1, p.getTotalPages());
        int numberOfElements = p.getNumberOfElements();
        long from = total == 0 ? 0 : ((long) (currentPage - 1) * size) + 1;
        long to = total == 0 ? 0 : (from + numberOfElements - 1);

        res.put("data", p.getContent());
        res.put("total", total);
        res.put("per_page", size);
        res.put("current_page", currentPage);
        res.put("last_page", lastPage);
        res.put("from", from);
        res.put("to", to);
        res.put("prev_page_url", currentPage > 1 ? currentPage - 1 : null);
        res.put("next_page_url", currentPage < lastPage ? currentPage + 1 : null);
        return res;
    }

    private String toJson(Object v) {
        if (v == null) return null;
        try {
            // Nếu đã giống chuỗi JSON thì giữ nguyên để tránh encode hai lần
            if (v instanceof String s) {
                String trimmed = s.trim();
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) return s; // array JSON
                if (trimmed.startsWith("{") && trimmed.endsWith("}")) return s; // object JSON
                // otherwise, serialize as JSON string
                return mapper.writeValueAsString(s);
            }
            // For collections/arrays/maps/POJOs, serialize directly
            return mapper.writeValueAsString(v);
        } catch (Exception e) {
            // Dự phòng: giá trị chuỗi
            return String.valueOf(v);
        }
    }

    private String normalizeDigitalCertificates(Object v) {
        if (v == null) return null;
        try {
            JsonNode node;
            if (v instanceof String s) {
                if (s.isBlank()) return null;
                node = mapper.readTree(s);
            } else {
                node = mapper.valueToTree(v);
            }
            if (node == null) return null;
            if (node.isArray()) {
                ArrayNode arr = (ArrayNode) node;
                for (JsonNode item : arr) ensureSigRegMethodInt(item);
                return mapper.writeValueAsString(arr);
            } else if (node.isObject()) {
                ensureSigRegMethodInt(node);
                return mapper.writeValueAsString(node);
            } else {
                return mapper.writeValueAsString(node);
            }
        } catch (Exception e) {
            // fallback to raw string
            return String.valueOf(v);
        }
    }

    private void ensureSigRegMethodInt(JsonNode node) {
        if (node == null || !node.isObject()) return;
        ObjectNode obj = (ObjectNode) node;
        String[] keys = new String[]{"sigRegMethod","sig_reg_method","method"};
        Integer value = null;
        for (String k : keys) {
            JsonNode v = obj.get(k);
            if (v == null) continue;
            if (v.isNumber()) { value = v.asInt(); break; }
            if (v.isTextual()) {
                String s = v.asText();
                if (s.matches("^\\d+$")) { value = Integer.parseInt(s); break; }
                String up = s.trim().toUpperCase(Locale.ROOT);
                if (up.equals("ADD")) { value = 1; break; }
                if (up.equals("EXTEND")) { value = 2; break; }
                if (up.equals("STOP")) { value = 3; break; }
            }
        }
        if (value == null) value = 1; // default ADD
        // translate legacy 0 -> 3
        if (value == 0) value = 3;
        obj.put("sigRegMethod", value);
        obj.remove(Arrays.asList("sig_reg_method","method"));
    }

    @GetMapping(value = "/{id}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getUnsignedXml(@PathVariable Long id) {
        Optional<RegisterInvoiceEntity> e = service.findById(id);
        if (e.isEmpty()) return ResponseEntity.notFound().build();
        // Authorization: ensure same company unless root
        UserEntity user = currentUser();
        if (user != null && user.getRole() != null && user.getRole() != 0) {
            if (user.getCompanyId() == null || !user.getCompanyId().equals(e.get().getCompanyId())) {
                return ResponseEntity.status(403).build();
            }
        }
        String xml = service.buildUnsignedXml(e.get());
        return ResponseEntity.ok(xml);
    }

    @GetMapping(value = "/{id}/download-xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> downloadXml(@PathVariable Long id) {
        Optional<RegisterInvoiceEntity> e = service.findById(id);
        if (e.isEmpty()) return ResponseEntity.notFound().build();
        UserEntity user = currentUser();
        if (user != null && user.getRole() != null && user.getRole() != 0) {
            if (user.getCompanyId() == null || !user.getCompanyId().equals(e.get().getCompanyId())) {
                return ResponseEntity.status(403).build();
            }
        }
        Optional<String> xmlOpt = service.getXmlForDownload(id);
        return xmlOpt.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        permission("register-invoice-save");
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(403).build();
        }
        Optional<RegisterInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        RegisterInvoiceEntity e = opt.get();
        // Allow delete only within company scope (or root)
        Integer role = user.getRole();
        boolean isRoot = role != null && role == 0;
        if (!isRoot && !user.getCompanyId().equals(e.getCompanyId())) {
            return ResponseEntity.status(403).build();
        }
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/sign")
    public ResponseEntity<RegisterInvoiceEntity> simulateSign(@PathVariable Long id) {
        permission("register-invoice-send");
        UserEntity user = currentUser();
        if (user == null) return ResponseEntity.status(403).build();
        Optional<RegisterInvoiceEntity> opt = service.findById(id);
        if (opt.isEmpty()) return ResponseEntity.notFound().build();
        RegisterInvoiceEntity entity = opt.get();
        Integer role = user.getRole();
        boolean isRoot = role != null && role == 0;
        if (!isRoot) {
            if (user.getCompanyId() == null || !user.getCompanyId().equals(entity.getCompanyId())) {
                return ResponseEntity.status(403).build();
            }
        }
        Optional<CompanyEntity> companyOpt = companyRepository.findById(entity.getCompanyId());
        if (companyOpt.isEmpty()) return ResponseEntity.badRequest().build();
        CompanyEntity company = companyOpt.get();
        String companyName = company.getName() != null ? company.getName() : "";
        String taxCode = company.getTaxcode() != null ? company.getTaxcode() : "";

        String unsignedXml = service.buildUnsignedXml(entity);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        String signedXml = injectSignatureIntoXml(unsignedXml, companyName, taxCode, now);

        // Per requirement: signature_info should display signer company name only
        String signatureInfo = companyName;

        Optional<RegisterInvoiceEntity> updated = service.attachSignedXml(id, signedXml, signatureInfo);
        return updated.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    // --- Helpers to inject a simulated signature block into XML ---
    private String injectSignatureIntoXml(String xml, String companyName, String taxCode, java.time.LocalDateTime signedAt) {
        if (xml == null) xml = "";
        String safeCompany = escapeXml(companyName);
        String safeTax = escapeXml(taxCode);
        String ts = signedAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String sigVal = java.util.UUID.randomUUID().toString().replace("-", "").toUpperCase();
        String block = "<Signature>"
                + tag("SignerName", safeCompany)
                + tag("SignerTaxCode", safeTax)
                + tag("SignedAt", ts)
                + tag("SignatureValue", "SIMULATED-" + sigVal)
                + "</Signature>";

        // Try to insert into <DSCKS><NNT>...</NNT></DSCKS>
        String marker = "<DSCKS><NNT>";
        int idx = xml.indexOf(marker);
        if (idx >= 0) {
            int closeIdx = xml.indexOf("</NNT>", idx);
            if (closeIdx >= 0) {
                String prefix = xml.substring(0, idx + marker.length());
                String suffix = xml.substring(closeIdx);
                return prefix + block + suffix;
            }
        }
        // Dự phòng: thay NNT rỗng
        String emptyNnt = "<NNT></NNT>";
        if (xml.contains(emptyNnt)) {
            return xml.replace(emptyNnt, "<NNT>" + block + "</NNT>");
        }
        // Last resort: append DSCKS at the end, before </TKhai>
        int endIdx = xml.lastIndexOf("</TKhai>");
        String dscks = "<DSCKS><NNT>" + block + "</NNT></DSCKS>";
        if (endIdx > 0) {
            return xml.substring(0, endIdx) + dscks + xml.substring(endIdx);
        }
        return xml + dscks;
    }

    private String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String tag(String name, String value) { return "<" + name + ">" + (value == null ? "" : value) + "</" + name + ">"; }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<HistoryDto>> getHistory(@PathVariable Long id) {
        UserEntity user = currentUser();
        if (user == null || user.getCompanyId() == null) {
            return ResponseEntity.status(403).build();
        }
        if (historyService == null) return ResponseEntity.ok(List.of());
        List<HistoryDto> rows = historyService.listByRegisterInvoice(user.getCompanyId(), id);
        return ResponseEntity.ok(rows);
    }
}
