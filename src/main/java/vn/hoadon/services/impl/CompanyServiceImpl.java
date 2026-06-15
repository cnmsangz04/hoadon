package vn.hoadon.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.hoadon.dto.company.CompanyFilterDTO;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.PermissionEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.entity.UserPermissionEntity;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.repositories.PermissionRepository;
import vn.hoadon.repositories.UserPermissionRepository;
import vn.hoadon.repositories.UserRepository;
import vn.hoadon.services.CompanyService;

import jakarta.persistence.criteria.Predicate;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CompanyServiceImpl implements CompanyService {

    private static final Logger log = LoggerFactory.getLogger(CompanyServiceImpl.class);

    private final CompanyRepository repo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserPermissionRepository userPermissionRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    public CompanyServiceImpl(CompanyRepository repo) {
        this.repo = repo;
    }
  
    @Override
    public Page<CompanyEntity> list(CompanyFilterDTO filter, Pageable pageable) {
        Specification<CompanyEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter != null) {
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }

                if (filter.getKeyword() != null && !filter.getKeyword().isEmpty()) {
                    String like = "%" + filter.getKeyword() + "%";
                    predicates.add(cb.or(
                        cb.like(root.get("domain"), like),
                        cb.like(root.get("taxcode"), like),
                        cb.like(root.get("name"), like),
                        cb.like(root.get("address"), like)
                    ));
                }

                if (filter.getCompanyId() != null) {
                    predicates.add(cb.equal(root.get("id"), filter.getCompanyId()));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return repo.findAll(spec, pageable);
    }

    @Override
    public CompanyEntity saveOrUpdate(CompanyEntity company) {
        if (company.getId() == null) {
            if (company.getPrefix() == null || company.getPrefix().isEmpty()) {
                company.setPrefix(generatePrefix());
            }
            CompanyEntity savedCompany = repo.save(company);

            // Auto-create default admin user for the new company
            createDefaultCompanyAdmin(savedCompany);

            return savedCompany;
        }

        CompanyEntity existing = repo.findById(company.getId())
                .orElseThrow(() -> new RuntimeException("Company not found"));

        if (company.getTaxcode() != null) existing.setTaxcode(company.getTaxcode());
        if (company.getEmail() != null) existing.setEmail(company.getEmail());
        if (company.getHotline() != null) existing.setHotline(company.getHotline());
        if (company.getStatus() != null) existing.setStatus(company.getStatus());
        if (company.getName() != null) existing.setName(company.getName());
        if (company.getAddress() != null) existing.setAddress(company.getAddress());

        return repo.save(existing);
    }
    
    @Override
    public void updateStatus(Long id, Integer status) {
        CompanyEntity existing = repo.findById(id)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        existing.setStatus(status != null && (status == 1 || status == 2) ? status : 0);
        repo.save(existing);
    }

    private String generatePrefix() {
        String prefix;
        do {
            prefix = "HD" + System.currentTimeMillis();
        } while (repo.existsByPrefix(prefix));
        return prefix;
    }

    private boolean isBcrypt(String v) {
        if (v == null) return false;
        if (v.length() == 60 && v.startsWith("$2")) return true;
        return v.matches("^\\$2[aby]\\$\\d{2}\\$.*");
    }

    private String encodeIfNeeded(String rawOrHash) {
        if (rawOrHash == null || rawOrHash.isBlank()) return rawOrHash;
        return isBcrypt(rawOrHash) ? rawOrHash : passwordEncoder.encode(rawOrHash);
    }

    @Override
    public Optional<CompanyEntity> findById(Long id) {
        return repo.findById(id);
    }

    @Override
    public void delete(Long id) {
        repo.deleteById(id);
    }

    // Create user (username=admin) for company and assign level=0 permissions
    private void createDefaultCompanyAdmin(CompanyEntity company) {
        if (company == null || company.getId() == null) return;

        // If an admin user already exists for this company with username 'admin', skip
        try {
            Optional<UserEntity> existingAdmin = userRepository.findByUsername("admin");
            if (existingAdmin.isPresent() && company.getId().equals(existingAdmin.get().getCompanyId())) {
                return;
            }
        } catch (Exception ignore) {}

        // Build user entity
        UserEntity user = new UserEntity();
        user.setCompanyId(company.getId());
        user.setUsername("admin");
        user.setName("Admin");
        user.setRole(1); // Admin
        user.setStatus((byte)1);
        String rawPassword = generateStrongPassword(14);
        user.setPassword(passwordEncoder.encode(rawPassword));

        UserEntity savedUser = userRepository.save(user);

        // Assign all permissions with level=0 and status=1
        List<PermissionEntity> basePerms = permissionRepository.findByLevelAndStatus(0, (byte)1);
        if (basePerms != null && !basePerms.isEmpty()) {
            List<UserPermissionEntity> assigns = new ArrayList<>(basePerms.size());
            for (PermissionEntity p : basePerms) {
                UserPermissionEntity up = new UserPermissionEntity();
                up.setUser(savedUser);
                up.setPermission(p);
                up.setAllowed((byte)1);
                assigns.add(up);
            }
            userPermissionRepository.saveAll(assigns);
        }
    }

    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String DIGIT = "23456789";
    private static final String SYM = "!@#$%^&*()-_=+[]{}:;,./?";
    private static final SecureRandom RNG = new SecureRandom();
    private String generateStrongPassword(int length) {
        String all = LOWER + UPPER + DIGIT + SYM;
        StringBuilder sb = new StringBuilder(length);
        // ensure at least one from each group
        sb.append(LOWER.charAt(RNG.nextInt(LOWER.length())));
        sb.append(UPPER.charAt(RNG.nextInt(UPPER.length())));
        sb.append(DIGIT.charAt(RNG.nextInt(DIGIT.length())));
        sb.append(SYM.charAt(RNG.nextInt(SYM.length())));
        for (int i = sb.length(); i < length; i++) {
            sb.append(all.charAt(RNG.nextInt(all.length())));
        }
        // simple shuffle
        char[] arr = sb.toString().toCharArray();
        for (int i = arr.length - 1; i > 0; i--) {
            int j = RNG.nextInt(i + 1);
            char t = arr[i];
            arr[i] = arr[j];
            arr[j] = t;
        }
        return new String(arr);
    }

    @Override
    public void sendAdminCredentials(Long companyId) {
        if (companyId == null) {
            throw new IllegalArgumentException("Thiếu thông tin công ty");
        }

        CompanyEntity company = repo.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công ty"));

        if (company.getEmail() == null || company.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Công ty chưa cấu hình email nhận thông tin");
        }

        // Find first admin user (role = 1) of this company
        List<UserEntity> users = userRepository.findByCompanyId(companyId);
        UserEntity admin = null;
        if (users != null) {
            for (UserEntity u : users) {
                if (u != null && Integer.valueOf(1).equals(u.getRole())) {
                    admin = u;
                    break;
                }
            }
        }

        if (admin == null) {
            throw new IllegalArgumentException("Không tìm thấy tài khoản quản trị cho công ty này");
        }

        // Reset password every time sending
        String rawPassword = generateStrongPassword(14);
        admin.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(admin);

        // Log the credentials being sent (for audit/debug)
        log.info("Sending admin credentials for companyId={}, username={}, password={}",
                companyId, admin.getUsername(), rawPassword);

        // Simulate sending email with credentials
        if (mailSender != null) {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(company.getEmail());
            msg.setSubject("Thông tin tài khoản quản trị hệ thống hóa đơn");
            StringBuilder body = new StringBuilder();
            body.append("Kính gửi Quý công ty \n\n");
            body.append("Thông tin tài khoản quản trị: \n");
            body.append("Tên đăng nhập: ").append(admin.getUsername()).append("\n");
            body.append("Mật khẩu: ").append(rawPassword).append("\n\n");
            body.append("Vui lòng đăng nhập và đổi mật khẩu sau khi sử dụng lần đầu.");
            msg.setText(body.toString());
            mailSender.send(msg);
        }
    }
}
