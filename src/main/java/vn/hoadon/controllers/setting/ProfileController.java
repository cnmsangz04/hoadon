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
        vn.hoadon.entity.CompanyEntity company = companyOpt.get();
        if (companyName != null) company.setName(companyName);
        if (companyAddress != null) company.setAddress(companyAddress);
        if (companyBusiness != null) company.setBusiness(companyBusiness);
        
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
        vn.hoadon.entity.LegalRepresentativeEntity rep = legalRepresentativeRepository.findByCompanyId(companyId)
                .orElseGet(vn.hoadon.entity.LegalRepresentativeEntity::new);
        rep.setCompanyId(companyId);
        rep.setFullname((String) body.getOrDefault("representName", null));
        rep.setGender((Integer) body.getOrDefault("representGender", null));
        Object date = body.get("representDateBirth");
        if (date instanceof String) {
            try {
                rep.setDateOfBirth(java.time.LocalDate.parse((String) date));
            } catch (Exception ignored) {}
        }
        rep.setCitizenId((String) body.getOrDefault("representCitizenIdent", null));
        rep.setPassportNo((String) body.getOrDefault("representPassPort", null));
        rep.setPhone((String) body.getOrDefault("representPhone", null));
        rep.setEmail((String) body.getOrDefault("representMail", null));
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
        vn.hoadon.entity.CompanyEntity company = companyOpt.get();
        if (body.containsKey("invoiceEmail")) company.setInvoiceEmail(body.get("invoiceEmail"));
        if (body.containsKey("invoicePhone")) company.setInvoicePhone(body.get("invoicePhone"));
        if (body.containsKey("invoiceFax")) company.setInvoiceFax(body.get("invoiceFax"));
        if (body.containsKey("invoiceWebsite")) company.setInvoiceWebsite(body.get("invoiceWebsite"));
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

        // Validate cơ bảns according to field expectations
        if (contactName == null || contactName.isEmpty()) {
            errors.put("contactName", Collections.singletonList("Vui lòng nhập họ và tên"));
        }

        if (contactMail == null || contactMail.isEmpty()) {
            errors.put("contactMail", Collections.singletonList("Vui lòng nhập email"));
        } else {
            // mẫu email rất đơn giản
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
            if (!contactMail.matches(emailRegex)) {
                errors.put("contactMail", Collections.singletonList("Email không hợp lệ"));
            }
        }

        if (contactPhone == null || contactPhone.isEmpty()) {
            errors.put("contactPhone", Collections.singletonList("Vui lòng nhập số điện thoại"));
        } else {
            String phoneRegex = "^[0-9+()\\-\\s]{6,20}$"; // chữ số, khoảng trắng, +, -, (), độ dài 6-20
            if (!contactPhone.matches(phoneRegex)) {
                errors.put("contactPhone", Collections.singletonList("Số điện thoại không hợp lệ"));
            }
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

        String bankAbbreviation = body.get("bankName"); // giá trị rút gọn từ v-select
        String bankNo = body.get("bankNo");
        String bankAddress = body.get("bankAddress");
        String bankBrand = body.get("bankBrand");

        // Tìm ngân hàng công ty đầu tiên hiện có hoặc tạo mới
        List<CompanyBankEntity> banks = companyBankRepository.findByCompany(company);
        CompanyBankEntity cb = banks.isEmpty() ? new CompanyBankEntity() : banks.get(0);
        cb.setCompany(company);

        if (bankAbbreviation != null && !bankAbbreviation.isBlank()) {
            Optional<BankEntity> bankOpt = bankService.findByAbbreviation(bankAbbreviation);
            bankOpt.ifPresent(bank -> cb.setBankName(bank.getName()));
            if (!bankOpt.isPresent()) {
                // Dự phòng: lưu đúng giá trị được truyền
                cb.setBankName(bankAbbreviation);
            }
        }
        if (bankNo != null) cb.setAccountNumber(bankNo);
        if (bankAddress != null) cb.setBankAddress(bankAddress);
        if (bankBrand != null) cb.setBankBrand(bankBrand);

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
