package vn.hoadon.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.hoadon.entity.CompanyEntity;
import vn.hoadon.entity.UserEntity;
import vn.hoadon.repositories.CompanyRepository;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth/info")
public class Info {

    @Autowired
    private CompanyRepository companyRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getInfo() {
        Map<String, Object> resp = new HashMap<>();

        // Resolve current user if authenticated
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null && auth.getPrincipal() instanceof UserEntity) {
            user = (UserEntity) auth.getPrincipal();
            Map<String, Object> u = new HashMap<>();
            u.put("username", user.getUsername());
            u.put("name", user.getName());
            u.put("avatar", user.getAvatar());
            u.put("role", user.getRole());
            u.put("companyId", user.getCompanyId());
            resp.put("user", u);
        } else {
            resp.put("user", null);
        }

        // Resolve company strictly by user's companyId; do NOT fallback to 1
        CompanyEntity company = null;
        Long companyId = (user != null) ? user.getCompanyId() : null;
        if (companyId != null) {
            company = companyRepository.findById(companyId).orElse(null);
        }
        if (company != null) {
            Map<String, Object> c = new HashMap<>();
            c.put("id", company.getId());
            c.put("name", company.getName());
            c.put("logo", company.getLogo());
            c.put("favicon", company.getFavicon());
            resp.put("company", c);
        } else {
            resp.put("company", null);
        }

        return ResponseEntity.ok(resp);
    }
}