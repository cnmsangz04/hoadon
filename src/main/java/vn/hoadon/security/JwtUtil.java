package vn.hoadon.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import vn.hoadon.entity.UserEntity;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET =
        "hoadon-prod-jwt-secret-2025-!@#A9fKxQ2LmP8wZr7YHnC4E6S1VdB";

    private static final long EXPIRATION_MS = 1000L * 60 * 60 * 4; // 4 hours

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    /**
     * Generate JWT token for user
     * Role convention:
     * 0 = Root
     * 1 = System Admin
     * 2 = Company Admin
     * 3 = Employee
     */
    public String generateToken(UserEntity user) {
        return generateToken(user, null);
    }

    public String generateToken(UserEntity user, String sessionId) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + EXPIRATION_MS);

        Long companyId = user.getCompanyId();
        Integer role = user.getRole();

        JwtBuilder builder = Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", role)
                .claim("companyId", companyId)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256);

        if (sessionId != null && !sessionId.isBlank()) {
            builder.claim("sid", sessionId);
        }

        return builder.compact();
    }

    /**
     * Parse and validate JWT claims
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
     * Helper methods (optional)
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

    public boolean isSystemAdmin(String token) {
        Object v = parseClaims(token).get("isSystemAdmin");
        return Boolean.TRUE.equals(v);
    }
}
