package vn.hoadon.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.hoadon.entity.CompanyAllowedIpEntity;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.CompanyAllowedIpRepository;
import vn.hoadon.repositories.CompanyRepository;
import vn.hoadon.util.ClientIpUtil;

import java.util.List;

@Service
public class CompanyIpSecurityService {

    private final CompanyRepository companyRepository;
    private final CompanyAllowedIpRepository allowedIpRepository;

    public CompanyIpSecurityService(CompanyRepository companyRepository,
                                    CompanyAllowedIpRepository allowedIpRepository) {
        this.companyRepository = companyRepository;
        this.allowedIpRepository = allowedIpRepository;
    }

    public boolean isAllowed(UserEntity user, String ipAddress) {
        if (user == null || user.getCompanyId() == null) return true;
        CompanyEntity company = companyRepository.findById(user.getCompanyId()).orElse(null);
        if (company == null || !Boolean.TRUE.equals(company.getIpSecurityEnabled())) return true;
        String ip = normalizeIp(ipAddress);
        if (ip.isBlank()) return false;
        return allowedIpRepository.existsByCompanyIdAndIpAddressIgnoreCaseAndStatus(user.getCompanyId(), ip, 1);
    }

    @Transactional
    public CompanyEntity setEnabled(UserEntity actor, boolean enabled, String currentIp) {
        Long companyId = requireCompanyId(actor);
        CompanyEntity company = companyRepository.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy công ty"));
        company.setIpSecurityEnabled(enabled);
        CompanyEntity saved = companyRepository.save(company);
        if (enabled) {
            ensureCurrentIpAllowed(companyId, actor, currentIp, true);
        }
        return saved;
    }

    @Transactional
    public CompanyAllowedIpEntity addIp(UserEntity actor, String ipAddress, String note) {
        Long companyId = requireCompanyId(actor);
        return ensureAllowedIp(companyId, actor, ipAddress, false, note);
    }

    @Transactional
    public void deleteIp(UserEntity actor, Long id) {
        Long companyId = requireCompanyId(actor);
        CompanyAllowedIpEntity item = allowedIpRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy IP"));
        if (!companyId.equals(item.getCompanyId())) {
            throw new IllegalArgumentException("IP không thuộc công ty hiện tại");
        }
        if (Boolean.TRUE.equals(item.getOriginalIp())) {
            throw new IllegalArgumentException("Không thể xóa IP gốc của công ty");
        }
        allowedIpRepository.delete(item);
    }

    public List<CompanyAllowedIpEntity> listIps(Long companyId) {
        if (companyId == null) return List.of();
        return allowedIpRepository.findByCompanyIdOrderByOriginalIpDescIdAsc(companyId);
    }

    public boolean isSecurityEnabled(Long companyId) {
        if (companyId == null) return false;
        return companyRepository.findById(companyId)
                .map(CompanyEntity::getIpSecurityEnabled)
                .orElse(false);
    }

    public String normalizeIp(String ipAddress) {
        return ClientIpUtil.normalize(ipAddress);
    }

    private void ensureCurrentIpAllowed(Long companyId, UserEntity actor, String currentIp, boolean original) {
        String ip = normalizeIp(currentIp);
        if (ip.isBlank()) {
            throw new IllegalArgumentException("Không xác định được IP hiện tại");
        }
        boolean hasOriginal = allowedIpRepository.findFirstByCompanyIdAndOriginalIpTrue(companyId).isPresent();
        ensureAllowedIp(companyId, actor, ip, original && !hasOriginal, "IP gốc khi kích hoạt bảo mật");
    }

    private CompanyAllowedIpEntity ensureAllowedIp(Long companyId, UserEntity actor, String ipAddress, boolean originalIp, String note) {
        String ip = normalizeIp(ipAddress);
        if (ip.isBlank()) {
            throw new IllegalArgumentException("Vui lòng nhập IP");
        }
        if (ip.length() > 64) {
            throw new IllegalArgumentException("IP quá dài");
        }

        CompanyAllowedIpEntity item = allowedIpRepository.findFirstByCompanyIdAndIpAddressIgnoreCase(companyId, ip)
                .orElseGet(CompanyAllowedIpEntity::new);
        if (item.getId() == null) {
            item.setCompanyId(companyId);
            item.setIpAddress(ip);
            item.setCreatedBy(actor != null ? actor.getId() : null);
        }
        item.setStatus(1);
        item.setOriginalIp(Boolean.TRUE.equals(item.getOriginalIp()) || originalIp);
        if (note != null && !note.trim().isEmpty()) {
            item.setNote(trim(note, 255));
        } else if (item.getNote() == null || item.getNote().isBlank()) {
            item.setNote(originalIp ? "IP gốc của công ty" : "IP được phép đăng nhập");
        }
        return allowedIpRepository.save(item);
    }

    private Long requireCompanyId(UserEntity actor) {
        if (actor == null || actor.getCompanyId() == null) {
            throw new IllegalArgumentException("Không xác định được công ty hiện tại");
        }
        return actor.getCompanyId();
    }

    private String trim(String value, int max) {
        String s = value == null ? "" : value.trim();
        return s.length() <= max ? s : s.substring(0, max);
    }
}
