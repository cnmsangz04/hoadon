package vn.hoadon.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import vn.hoadon.entity.UserEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtUtil {

    private static final long EXPIRATION_MS = 1000L * 60 * 60 * 4; // 4 giờ
    private static final long REMEMBER_EXPIRATION_MS = 1000L * 60 * 60 * 24 * 30; // 30 ngày

    private final Key key;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET phải có ít nhất 32 byte");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Tạo JWT cho user.
     * Quy ước role: 0 = Quản trị viên toàn quyền, 1 = Quản trị viên hệ thống,
     * 2 = Quản lý doanh nghiệp, 3 = Nhân viên doanh nghiệp.
     */
    public String generateToken(UserEntity user) {
        return generateToken(user, null);
    }

    public String generateToken(UserEntity user, String sessionId) {
        return generateToken(user, sessionId, false);
    }

    public String generateToken(UserEntity user, String sessionId, boolean remember) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + (remember ? REMEMBER_EXPIRATION_MS : EXPIRATION_MS));

        Long companyId = user.getCompanyId();
        Integer role = user.getRole();

        JwtBuilder builder = Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", role)
                .claim("companyId", companyId)
                .claim("adminAccess", user.canAccessAdminArea())
                .claim("remember", remember)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256);

        if (sessionId != null && !sessionId.isBlank()) {
            builder.claim("sid", sessionId);
        }

        return builder.compact();
    }

    /**
     * Phân tích và kiểm tra claim trong JWT.
     */
    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Lấy username từ token
     */
    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Các hàm hỗ trợ đọc claim.
     */
    public Integer getRole(String token) {
        Object role = parseClaims(token).get("role");
        return role != null ? Integer.valueOf(role.toString()) : null;
    }

    public Long getCompanyId(String token) {
        Object cid = parseClaims(token).get("companyId");
        return cid != null ? Long.valueOf(cid.toString()) : null;
    }

    public String getSessionId(String token) {
        Object sid = parseClaims(token).get("sid");
        return sid != null ? sid.toString() : null;
    }

    public LocalDateTime getExpiration(String token) {
        Date date = parseClaims(token).getExpiration();
        return date != null
                ? LocalDateTime.ofInstant(Instant.ofEpochMilli(date.getTime()), ZoneId.systemDefault())
                : null;
    }

}
