package vn.hoadon.controllers.setting;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.hoadon.dto.profile.ProfileDTO;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.LegalRepresentativeEntity;
import vn.hoadon.entity.TaxAuthorityEntity;
import vn.hoadon.entity.CompanyBankEntity;
import vn.hoadon.entity.BankEntity;
import vn.hoadon.services.TaxAuthorityService;
import vn.hoadon.services.BankService;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.LegalRepresentativeRepository;
import vn.hoadon.repositories.CompanyBankRepository;
import vn.hoadon.util.UploadPath;

import java.util.*;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.UserEntity;

@RestController
@RequestMapping("/v1/setting/profile")
public class ProfileController extends BaseController {

    private final CompanyRepository companyRepository;
    private final LegalRepresentativeRepository legalRepresentativeRepository;
    private final TaxAuthorityService taxAuthorityService;
    private final BankService bankService;
    private final CompanyBankRepository companyBankRepository;

    private static final String EMAIL_REGEX = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$";
    private static final String PHONE_REGEX = "^(0\\d{9,10}|\\+?\\d{9,15})$";

    public ProfileController(
            CompanyRepository companyRepository,
            LegalRepresentativeRepository legalRepresentativeRepository,
            TaxAuthorityService taxAuthorityService,
            BankService bankService,
            CompanyBankRepository companyBankRepository
    ) {
        this.companyRepository = companyRepository;
        this.legalRepresentativeRepository = legalRepresentativeRepository;
        this.taxAuthorityService = taxAuthorityService;
        this.bankService = bankService;
        this.companyBankRepository = companyBankRepository;
    }

    private String trim(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private void addRequired(Map<String, List<String>> errors, String field, String value, String label) {
        if (value == null || value.isBlank()) {
            errors.put(field, Collections.singletonList("Vui lòng nhập " + label));
        }
    }

    private ResponseEntity<?> validationError(Map<String, List<String>> errors) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("errors", errors);
        return ResponseEntity.status(422).body(resp);
    }

    private boolean isValidEmail(String value) {
        return value != null && value.matches(EMAIL_REGEX);
    }

    private boolean isValidPhone(String value) {
        if (value == null) return false;
        String normalized = value.replaceAll("[\\s().-]", "");
        return normalized.matches(PHONE_REGEX);
    }

    private boolean isValidCitizenIdent(String value) {
        return value == null || value.isBlank() || value.matches("^(\\d{9}|\\d{12})$");
    }

    private boolean isValidPassport(String value) {
        return value == null || value.isBlank() || value.matches("^[A-Za-z0-9]{6,20}$");
    }

    private boolean isValidWebsite(String value) {
        if (value == null || value.isBlank() || value.matches(".*\\s+.*")) return false;
        String normalized = value.matches("(?i)^https?://.*") ? value : "https://" + value;
        try {
            java.net.URI uri = java.net.URI.create(normalized);
            String host = uri.getHost();
            return host != null && host.contains(".");
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isValidBankNo(String value) {
        return value != null && value.matches("^[0-9A-Za-z.-]{4,32}$");
    }

    private boolean isAllowedImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return true;
        String name = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase(Locale.ROOT);
        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
    }

    private LocalDate parseProfileDate(Object value) {
        String text = trim(value);
        if (text.length() >= 10) {
            text = text.substring(0, 10);
        }
        try {
            return LocalDate.parse(text);
        } catch (Exception e) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    // Cung cấp dữ liệu tùy chọn: ngân hàng và cơ quan thuế (tỉnh/thành)
    @PostMapping("/ini")
    public ResponseEntity<Map<String, Object>> init() {
        permission("setting-profile");
        Map<String, Object> map = new HashMap<>();
        map.put("banks", bankService.list(1));
        map.put("taxAuthorities", taxAuthorityService.listCities());
        return ResponseEntity.ok(map);
    }

    // Trả dữ liệu hồ sơ cho công ty hiện tại theo companyId của user đã xác thực
    @PostMapping("/get")
    public ResponseEntity<ProfileDTO> profile() {
        permission("setting-profile");
        // Lấy user đã xác thực
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long companyId = null;
        if (auth != null && auth.getPrincipal() instanceof UserEntity) {
            companyId = ((UserEntity) auth.getPrincipal()).getCompanyId();
        }

        if (companyId == null) {
            // Không có user hoặc chưa gắn công ty; trả DTO rỗng
            return ResponseEntity.ok(new ProfileDTO());
        }

        Optional<CompanyEntity> companyOpt = companyRepository.findById(companyId);
        ProfileDTO dto = new ProfileDTO();
        companyOpt.ifPresent(company -> {
            dto.companyId = company.getId();
            dto.taxCode = company.getTaxcode();
            dto.companyName = company.getName();
            dto.companyBusiness = company.getBusiness();
            dto.companyAddress = company.getAddress();
            dto.companyLogo = company.getLogo();
            dto.companyFavicon = company.getFavicon();

            dto.invoiceEmail = company.getInvoiceEmail();
            dto.invoicePhone = company.getInvoicePhone();
            dto.invoiceFax = company.getInvoiceFax();
            dto.invoiceWebsite = company.getInvoiceWebsite();

            // Các trường liên hệ cho hồ sơ thuế
            dto.contactName = company.getContactName();
            dto.contactMail = company.getContactMail();
            dto.contactPhone = company.getContactPhone();
            dto.contactAddress = company.getContactAddress();

            // Mapping cơ quan thuế (lưu mã và tên hiển thị)
            if (company.getTaxAuthorityCity() != null) {
                TaxAuthorityEntity city = company.getTaxAuthorityCity();
                dto.taxAuthorityCity = city.getCode();
                dto.bladeTaxAuthorityCity = city.getName();
            }
            if (company.getTaxAuthority() != null) {
                TaxAuthorityEntity name = company.getTaxAuthority();
                dto.taxAuthorityName = name.getCode();
                dto.bladeTaxAuthorityName = name.getName();
            }

            // Thông tin ngân hàng (dùng ngân hàng đầu tiên nếu có nhiều)
            List<CompanyBankEntity> companyBanks = company.getCompanyBanks();
            if (companyBanks != null && !companyBanks.isEmpty()) {
                CompanyBankEntity cb = companyBanks.get(0);
                dto.bankNo = cb.getAccountNumber();
                dto.bankAddress = cb.getBankAddress();
                dto.bankBrand = cb.getBankBrand();
                // Thử ánh xạ tên ngân hàng sang mã viết tắt cho ô chọn
                if (cb.getBankName() != null) {
                    bankService.findByName(cb.getBankName()).ifPresentOrElse(
                        b -> {
                            dto.bankAbbreviation = b.getAbbreviation();
                            dto.bankName = b.getName();
                        },
                        () -> dto.bankName = cb.getBankName()
                    );
                }
            }
        });

        // Tải người đại diện pháp luật theo companyId
        legalRepresentativeRepository.findByCompanyId(companyId).ifPresent(rep -> {
            dto.representName = rep.getFullname();
            dto.representGender = rep.getGender();
            dto.representDateBirth = rep.getDateOfBirth();
            dto.representCitizenIdent = rep.getCitizenId();
            dto.representPassPort = rep.getPassportNo();
            dto.representPhone = rep.getPhone();
            dto.representMail = rep.getEmail();
        });

        return ResponseEntity.ok(dto);
    }

    // Tải cơ quan thuế theo mã tỉnh/thành
    @PostMapping("/get-tax-authority")
    public ResponseEntity<?> getTaxAuthority(@RequestBody Map<String, Integer> body) {
        permission("setting-profile");
        Integer parentCode = body.get("parentCode");
        if (parentCode == null) return ResponseEntity.ok(Collections.emptyList());
        Optional<TaxAuthorityEntity> city = taxAuthorityService.findByCode(parentCode);
        if (!city.isPresent()) {
            return ResponseEntity.ok(0);
        }
        List<TaxAuthorityEntity> names = taxAuthorityService.listByParentActive(city.get().getId());
        return ResponseEntity.ok(names);
    }

    // Cập nhật thông tin công ty và file upload
    @PostMapping(value = "/update-info")
    public ResponseEntity<?> updateInfo(
            @RequestParam(value = "companyName", required = false) String companyName,
            @RequestParam(value = "companyAddress", required = false) String companyAddress,
            @RequestParam(value = "companyBusiness", required = false) String companyBusiness,
            @RequestParam(value = "logo", required = false) MultipartFile logo,
            @RequestParam(value = "favicon", required = false) MultipartFile favicon
    ) {
        permission("setting-profile");
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long companyId = null;
        if (auth != null && auth.getPrincipal() instanceof vn.hoadon.entity.UserEntity) {
            companyId = ((vn.hoadon.entity.UserEntity) auth.getPrincipal()).getCompanyId();
        }
        if (companyId == null) {
            return ResponseEntity.status(403).body("Không xác định được công ty từ người dùng hiện tại");
        }
        Optional<vn.hoadon.entity.CompanyEntity> companyOpt = companyRepository.findById(companyId);
        if (!companyOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        String cleanCompanyName = trim(companyName);
        String cleanCompanyAddress = trim(companyAddress);
        String cleanCompanyBusiness = trim(companyBusiness);
        Map<String, List<String>> errors = new HashMap<>();
        addRequired(errors, "companyName", cleanCompanyName, "tên đơn vị");
        addRequired(errors, "companyAddress", cleanCompanyAddress, "địa chỉ");
        addRequired(errors, "companyBusiness", cleanCompanyBusiness, "ngành nghề");
        if (!isAllowedImageFile(logo)) {
            errors.put("logo", Collections.singletonList("Logo chỉ hỗ trợ PNG, JPG hoặc JPEG"));
        }
        if (!isAllowedImageFile(favicon)) {
            errors.put("favicon", Collections.singletonList("Favicon chỉ hỗ trợ PNG, JPG hoặc JPEG"));
        }
        if (!errors.isEmpty()) {
            return validationError(errors);
        }

        vn.hoadon.entity.CompanyEntity company = companyOpt.get();
        company.setName(cleanCompanyName);
        company.setAddress(cleanCompanyAddress);
        company.setBusiness(cleanCompanyBusiness);
        
        // Lưu logo
        if (logo != null && !logo.isEmpty()) {
            try {
                Path dir = UploadPath.resolveCompanyTypeDir(companyId, "logo");
                String ext = Optional.ofNullable(logo.getOriginalFilename())
                        .filter(f -> f.contains("."))
                        .map(f -> f.substring(f.lastIndexOf('.')))
                        .orElse(".png");
                String fileName = UUID.randomUUID() + ext;
                Path file = dir.resolve(fileName);
                Files.write(file, logo.getBytes(), StandardOpenOption.CREATE_NEW);
                company.setLogo(UploadPath.publicUrl(companyId, "logo", fileName));
            } catch (IOException e) {
                return ResponseEntity.status(500).body("Lỗi lưu logo: " + e.getMessage());
            }
        }
        // Lưu favicon
        if (favicon != null && !favicon.isEmpty()) {
            try {
                Path dir = UploadPath.resolveCompanyTypeDir(companyId, "favicon");
                String ext = Optional.ofNullable(favicon.getOriginalFilename())
                        .filter(f -> f.contains("."))
                        .map(f -> f.substring(f.lastIndexOf('.')))
                        .orElse(".png");
                String fileName = UUID.randomUUID() + ext;
                Path file = dir.resolve(fileName);
                Files.write(file, favicon.getBytes(), StandardOpenOption.CREATE_NEW);
                company.setFavicon(UploadPath.publicUrl(companyId, "favicon", fileName));
            } catch (IOException e) {
                return ResponseEntity.status(500).body("Lỗi lưu favicon: " + e.getMessage());
            }
        }

        companyRepository.save(company);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update-represent")
    public ResponseEntity<?> updateRepresent(@RequestBody Map<String, Object> body) {
        permission("setting-profile");
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long companyId = null;
        if (auth != null && auth.getPrincipal() instanceof vn.hoadon.entity.UserEntity) {
            companyId = ((vn.hoadon.entity.UserEntity) auth.getPrincipal()).getCompanyId();
        }
        if (companyId == null) {
            return ResponseEntity.status(403).body("Không xác định được công ty từ người dùng hiện tại");
        }
        String representName = trim(body.get("representName"));
        String representPhone = trim(body.get("representPhone"));
        String representCitizenIdent = trim(body.get("representCitizenIdent"));
        String representPassPort = trim(body.get("representPassPort"));
        String representMail = trim(body.get("representMail"));
        Integer representGender = parseInteger(body.get("representGender"));
        LocalDate representDateBirth = parseProfileDate(body.get("representDateBirth"));

        Map<String, List<String>> errors = new HashMap<>();
        addRequired(errors, "representName", representName, "họ và tên");
        addRequired(errors, "representPhone", representPhone, "số điện thoại");
        if (!errors.containsKey("representPhone") && !isValidPhone(representPhone)) {
            errors.put("representPhone", Collections.singletonList("Số điện thoại không hợp lệ"));
        }
        if (!isValidCitizenIdent(representCitizenIdent)) {
            errors.put("representCitizenIdent", Collections.singletonList("Căn cước công dân phải gồm 9 hoặc 12 chữ số"));
        }
        if (!isValidPassport(representPassPort)) {
            errors.put("representPassPort", Collections.singletonList("Số hộ chiếu chỉ gồm chữ và số, từ 6 đến 20 ký tự"));
        }
        if (representDateBirth == null || representDateBirth.isBefore(LocalDate.of(1900, 1, 1)) || representDateBirth.isAfter(LocalDate.now())) {
            errors.put("manualDate", Collections.singletonList("Ngày sinh không hợp lệ"));
        }
        if (representGender == null || (representGender != 0 && representGender != 1)) {
            errors.put("representGender", Collections.singletonList("Vui lòng chọn giới tính"));
        }
        addRequired(errors, "representMail", representMail, "email");
        if (!errors.containsKey("representMail") && !isValidEmail(representMail)) {
            errors.put("representMail", Collections.singletonList("Email không hợp lệ"));
        }
        if (!errors.isEmpty()) {
            return validationError(errors);
        }

        vn.hoadon.entity.LegalRepresentativeEntity rep = legalRepresentativeRepository.findByCompanyId(companyId)
                .orElseGet(vn.hoadon.entity.LegalRepresentativeEntity::new);
        rep.setCompanyId(companyId);
        rep.setFullname(representName);
        rep.setGender(representGender);
        rep.setDateOfBirth(representDateBirth);
        rep.setCitizenId(representCitizenIdent.isBlank() ? null : representCitizenIdent);
        rep.setPassportNo(representPassPort.isBlank() ? null : representPassPort);
        rep.setPhone(representPhone);
        rep.setEmail(representMail);
        legalRepresentativeRepository.save(rep);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update-info-invoice")
    public ResponseEntity<?> updateInvoice(@RequestBody Map<String, String> body) {
        permission("setting-profile");
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long companyId = null;
        if (auth != null && auth.getPrincipal() instanceof vn.hoadon.entity.UserEntity) {
            companyId = ((vn.hoadon.entity.UserEntity) auth.getPrincipal()).getCompanyId();
        }
        if (companyId == null) {
            return ResponseEntity.status(403).body("Không xác định được công ty từ người dùng hiện tại");
        }
        Optional<vn.hoadon.entity.CompanyEntity> companyOpt = companyRepository.findById(companyId);
        if (!companyOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        String invoiceEmail = trim(body.get("invoiceEmail"));
        String invoicePhone = trim(body.get("invoicePhone"));
        String invoiceFax = trim(body.get("invoiceFax"));
        String invoiceWebsite = trim(body.get("invoiceWebsite"));

        Map<String, List<String>> errors = new HashMap<>();
        addRequired(errors, "invoiceEmail", invoiceEmail, "email");
        if (!errors.containsKey("invoiceEmail") && !isValidEmail(invoiceEmail)) {
            errors.put("invoiceEmail", Collections.singletonList("Email không hợp lệ"));
        }
        addRequired(errors, "invoicePhone", invoicePhone, "số điện thoại");
        if (!errors.containsKey("invoicePhone") && !isValidPhone(invoicePhone)) {
            errors.put("invoicePhone", Collections.singletonList("Số điện thoại không hợp lệ"));
        }
        addRequired(errors, "invoiceFax", invoiceFax, "số fax");
        if (!errors.containsKey("invoiceFax") && !invoiceFax.matches("^[0-9+().\\-\\s]{6,20}$")) {
            errors.put("invoiceFax", Collections.singletonList("Số fax không hợp lệ"));
        }
        addRequired(errors, "invoiceWebsite", invoiceWebsite, "website");
        if (!errors.containsKey("invoiceWebsite") && !isValidWebsite(invoiceWebsite)) {
            errors.put("invoiceWebsite", Collections.singletonList("Website không hợp lệ"));
        }
        if (!errors.isEmpty()) {
            return validationError(errors);
        }

        vn.hoadon.entity.CompanyEntity company = companyOpt.get();
        company.setInvoiceEmail(invoiceEmail);
        company.setInvoicePhone(invoicePhone);
        company.setInvoiceFax(invoiceFax);
        company.setInvoiceWebsite(invoiceWebsite);
        companyRepository.save(company);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update-contact")
    public ResponseEntity<?> updateContact(@RequestBody Map<String, String> body) {
        permission("setting-profile");
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long companyId = null;
        if (auth != null && auth.getPrincipal() instanceof vn.hoadon.entity.UserEntity) {
            companyId = ((vn.hoadon.entity.UserEntity) auth.getPrincipal()).getCompanyId();
        }
        if (companyId == null) {
            return ResponseEntity.status(403).body("Không xác định được công ty từ người dùng hiện tại");
        }
        Optional<vn.hoadon.entity.CompanyEntity> companyOpt = companyRepository.findById(companyId);
        if (!companyOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        // Lấy và trim input
        String contactName = Optional.ofNullable(body.get("contactName")).map(String::trim).orElse(null);
        String contactMail = Optional.ofNullable(body.get("contactMail")).map(String::trim).orElse(null);
        String contactPhone = Optional.ofNullable(body.get("contactPhone")).map(String::trim).orElse(null);
        String contactAddress = Optional.ofNullable(body.get("contactAddress")).map(String::trim).orElse(null);

        Map<String, List<String>> errors = new HashMap<>();

        // Validate các trường liên hệ bắt buộc.
        if (contactName == null || contactName.isEmpty()) {
            errors.put("contactName", Collections.singletonList("Vui lòng nhập họ và tên"));
        }

        if (contactMail == null || contactMail.isEmpty()) {
            errors.put("contactMail", Collections.singletonList("Vui lòng nhập email"));
        } else if (!isValidEmail(contactMail)) {
            errors.put("contactMail", Collections.singletonList("Email không hợp lệ"));
        }

        if (contactPhone == null || contactPhone.isEmpty()) {
            errors.put("contactPhone", Collections.singletonList("Vui lòng nhập số điện thoại"));
        } else if (!isValidPhone(contactPhone)) {
            errors.put("contactPhone", Collections.singletonList("Số điện thoại không hợp lệ"));
        }

        if (contactAddress == null || contactAddress.isEmpty()) {
            errors.put("contactAddress", Collections.singletonList("Vui lòng nhập địa chỉ"));
        }

        if (!errors.isEmpty()) {
            Map<String, Object> resp = new HashMap<>();
            resp.put("errors", errors);
            return ResponseEntity.status(422).body(resp);
        }

        vn.hoadon.entity.CompanyEntity company = companyOpt.get();
        company.setContactName(contactName);
        company.setContactMail(contactMail);
        company.setContactPhone(contactPhone);
        company.setContactAddress(contactAddress);
        companyRepository.save(company);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update-bank")
    public ResponseEntity<?> updateBank(@RequestBody Map<String, String> body) {
        permission("setting-profile");
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long companyId = null;
        if (auth != null && auth.getPrincipal() instanceof vn.hoadon.entity.UserEntity) {
            companyId = ((vn.hoadon.entity.UserEntity) auth.getPrincipal()).getCompanyId();
        }
        if (companyId == null) {
            return ResponseEntity.status(403).body("Không xác định được công ty từ người dùng hiện tại");
        }
        Optional<vn.hoadon.entity.CompanyEntity> companyOpt = companyRepository.findById(companyId);
        if (!companyOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        vn.hoadon.entity.CompanyEntity company = companyOpt.get();

        String bankAbbreviation = trim(body.get("bankName")); // giá trị rút gọn từ v-select
        String bankNo = trim(body.get("bankNo"));
        String bankAddress = trim(body.get("bankAddress"));
        String bankBrand = trim(body.get("bankBrand"));

        Map<String, List<String>> errors = new HashMap<>();
        addRequired(errors, "bankNo", bankNo, "số tài khoản ngân hàng");
        if (!errors.containsKey("bankNo") && !isValidBankNo(bankNo)) {
            errors.put("bankNo", Collections.singletonList("Số tài khoản ngân hàng không hợp lệ"));
        }
        if (bankAbbreviation.isBlank()) {
            errors.put("bankName", Collections.singletonList("Vui lòng chọn tên ngân hàng"));
        } else if (bankService.findByAbbreviation(bankAbbreviation).isEmpty()) {
            errors.put("bankName", Collections.singletonList("Ngân hàng không hợp lệ"));
        }
        if (bankBrand.length() > 255) {
            errors.put("bankBrand", Collections.singletonList("Chi nhánh không được vượt quá 255 ký tự"));
        }
        if (!errors.isEmpty()) {
            return validationError(errors);
        }

        // Tìm ngân hàng công ty đầu tiên hiện có hoặc tạo mới
        List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
        CompanyBankEntity cb = banks.isEmpty() ? new CompanyBankEntity() : banks.get(0);
        cb.setCompany(company);

        if (bankAbbreviation != null && !bankAbbreviation.isBlank()) {
            Optional<BankEntity> bankOpt = bankService.findByAbbreviation(bankAbbreviation);
            bankOpt.ifPresent(bank -> cb.setBankName(bank.getName()));
        }
        cb.setAccountNumber(bankNo);
        cb.setBankAddress(bankAddress.isBlank() ? null : bankAddress);
        cb.setBankBrand(bankBrand.isBlank() ? null : bankBrand);

        companyBankRepository.save(cb);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/update-tax-authority")
    public ResponseEntity<?> updateTaxAuthority(@RequestBody Map<String, Integer> body) {
        permission("setting-profile");
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        Long companyId = null;
        if (auth != null && auth.getPrincipal() instanceof vn.hoadon.entity.UserEntity) {
            companyId = ((vn.hoadon.entity.UserEntity) auth.getPrincipal()).getCompanyId();
        }
        if (companyId == null) {
            return ResponseEntity.status(403).body("Không xác định được công ty từ người dùng hiện tại");
        }
        Optional<vn.hoadon.entity.CompanyEntity> companyOpt = companyRepository.findById(companyId);
        if (!companyOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        vn.hoadon.entity.CompanyEntity company = companyOpt.get();
        Integer cityCode = body.get("taxAuthorityCity");
        Integer nameCode = body.get("taxAuthorityName");
        Map<String, List<String>> errors = new HashMap<>();
        if (cityCode == null) {
            errors.put("taxAuthorityCity", Collections.singletonList("Vui lòng chọn cục thuế Tỉnh/Thành"));
        } else if (taxAuthorityService.findByCode(cityCode).isEmpty()) {
            errors.put("taxAuthorityCity", Collections.singletonList("Cục thuế Tỉnh/Thành không hợp lệ"));
        }
        if (nameCode == null) {
            errors.put("taxAuthorityName", Collections.singletonList("Vui lòng chọn cơ quan thuế quản lý"));
        } else if (taxAuthorityService.findByCode(nameCode).isEmpty()) {
            errors.put("taxAuthorityName", Collections.singletonList("Cơ quan thuế quản lý không hợp lệ"));
        }
        if (!errors.isEmpty()) {
            return validationError(errors);
        }
        if (cityCode != null) {
            taxAuthorityService.findByCode(cityCode).ifPresent(company::setTaxAuthorityCity);
        }
        if (nameCode != null) {
            taxAuthorityService.findByCode(nameCode).ifPresent(company::setTaxAuthority);
        }
        companyRepository.save(company);
        return ResponseEntity.ok().build();
    }
}
