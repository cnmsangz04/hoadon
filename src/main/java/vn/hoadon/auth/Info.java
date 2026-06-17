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

        // Lấy user hiện tại nếu đã xác thực
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null && auth.getPrincipal() instanceof UserEntity) {
            user = (UserEntity) auth.getPrincipal();
            Map<String, Object> u = new HashMap<>();
            u.put("id", user.getId());
            u.put("username", user.getUsername());
            u.put("name", user.getName());
            u.put("avatar", user.getAvatar());
            u.put("role", user.getRole());
            u.put("companyId", user.getCompanyId());
            u.put("adminScope", user.getAdminScope());
            u.put("rootCompanyAdmin", user.isRootCompanyAdmin());
            u.put("adminAccess", user.canAccessAdminArea());
            resp.put("user", u);
        } else {
            resp.put("user", null);
        }

        // Lấy công ty đúng theo companyId của user; không fallback về 1
        CompanyEntity company = null;
        Long companyId = (user != null) ? user.getCompanyId() : null;
        if (companyId != null) {
            company = companyRepository.findById(companyId).orElse(null);
        }
        if (company != null) {
            Map<String, Object> c = new HashMap<>();
            c.put("id", company.getId());
            c.put("name", company.getName());
            c.put("taxcode", company.getTaxcode());
            c.put("taxCode", company.getTaxcode());
            c.put("address", company.getAddress());
            c.put("business", company.getBusiness());
            c.put("email", company.getEmail());
            c.put("hotline", company.getHotline());
            c.put("invoiceEmail", company.getInvoiceEmail());
            c.put("invoicePhone", company.getInvoicePhone());
            c.put("invoiceFax", company.getInvoiceFax());
            c.put("invoiceWebsite", company.getInvoiceWebsite());
            c.put("contactMail", company.getContactMail());
            c.put("logo", company.getLogo());
            c.put("favicon", company.getFavicon());
            c.put("status", company.getStatus());
            resp.put("company", c);
        } else {
            resp.put("company", null);
        }

        return ResponseEntity.ok(resp);
    }
}
