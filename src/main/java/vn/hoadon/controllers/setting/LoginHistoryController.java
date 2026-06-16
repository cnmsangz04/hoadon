package vn.hoadon.controllers.setting;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.controllers.base.BaseController;
import vn.hoadon.entity.LoginHistoryEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.LoginHistoryRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/setting/login-history")
public class LoginHistoryController extends BaseController {

    private final LoginHistoryRepository loginHistoryRepository;

    public LoginHistoryController(LoginHistoryRepository loginHistoryRepository) {
        this.loginHistoryRepository = loginHistoryRepository;
    }

    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        UserEntity actor = requireSettingManager();
        Long companyId = actor.getCompanyId();
        boolean root = isRoot(actor);
        Pageable pageable = PageRequest.of(
                Math.max(0, page),
                Math.max(1, size),
                Sort.by(Sort.Direction.DESC, "loginAt")
        );

        Page<LoginHistoryEntity> result = loginHistoryRepository.findAll((rootEntity, query, cb) -> {
            Predicate predicate = cb.conjunction();
            if (!root && companyId != null) {
                predicate = cb.and(predicate, cb.equal(rootEntity.get("companyId"), companyId));
            }
            String q = keyword == null ? "" : keyword.trim().toLowerCase();
            if (!q.isEmpty()) {
                String like = "%" + q + "%";
                Predicate search = cb.or(
                        cb.like(cb.lower(rootEntity.get("username")), like),
                        cb.like(cb.lower(rootEntity.get("ipAddress")), like),
                        cb.like(cb.lower(rootEntity.get("userAgent")), like)
                );
                predicate = cb.and(predicate, search);
            }
            return predicate;
        }, pageable);

        return toPaginationResponse(result.map(this::toDto));
    }

    private LoginHistoryDto toDto(LoginHistoryEntity entity) {
        LoginHistoryDto dto = new LoginHistoryDto();
        dto.setId(entity.getId());
        dto.setCompanyId(entity.getCompanyId());
        dto.setUserId(entity.getUserId());
        dto.setUsername(entity.getUsername());
        dto.setIpAddress(entity.getIpAddress());
        dto.setUserAgent(entity.getUserAgent());
        dto.setLoginType(entity.getLoginType());
        dto.setLoginAt(entity.getLoginAt());
        return dto;
    }

    private Map<String, Object> toPaginationResponse(Page<LoginHistoryDto> page) {
        Map<String, Object> res = new HashMap<>();
        long total = page.getTotalElements();
        int size = page.getSize();
        int currentPage = page.getNumber() + 1;
        int lastPage = Math.max(1, page.getTotalPages());
        int count = page.getNumberOfElements();
        long from = total == 0 ? 0 : ((long) (currentPage - 1) * size) + 1;
        long to = total == 0 ? 0 : from + count - 1;
        res.put("data", page.getContent());
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

    private UserEntity requireSettingManager() {
        UserEntity actor = currentUser();
        if (actor == null || actor.getRole() == null || actor.getRole() >= 2) {
            throw new AccessDeniedException("Bạn không có quyền xem lịch sử đăng nhập");
        }
        permission("setting-login-history");
        return actor;
    }

    private boolean isRoot(UserEntity actor) {
        return actor != null && actor.getRole() != null && actor.getRole() == 0;
    }

    public static class LoginHistoryDto {
        private Long id;
        private Long companyId;
        private Long userId;
        private String username;
        private String ipAddress;
        private String userAgent;
        private String loginType;
        private LocalDateTime loginAt;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getCompanyId() { return companyId; }
        public void setCompanyId(Long companyId) { this.companyId = companyId; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public String getUserAgent() { return userAgent; }
        public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
        public String getLoginType() { return loginType; }
        public void setLoginType(String loginType) { this.loginType = loginType; }
        public LocalDateTime getLoginAt() { return loginAt; }
        public void setLoginAt(LocalDateTime loginAt) { this.loginAt = loginAt; }
    }
}
