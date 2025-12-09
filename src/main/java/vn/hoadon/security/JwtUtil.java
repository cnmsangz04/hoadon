package vn.hoadon.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {
    private final Key key = Keys.hmacShaKeyFor("replace-this-with-a-very-long-secret-key-should-be-256-bit".getBytes());
    private final long expirationMs = 1000 * 60 * 60 * 4; // 4 hours

    public String generateToken(Long userId, String username, Integer role) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .setSubject(username)
                .setClaims(Map.of("userId", userId, "role", role))
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Jws<Claims> validateToken(String token) throws JwtException {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

    public String getUsername(String token) {
        return validateToken(token).getBody().getSubject();
    }

    public Long getUserId(String token){
        Object o = validateToken(token).getBody().get("userId");
        if(o instanceof Integer) return ((Integer)o).longValue();
        if(o instanceof Long) return (Long)o;
        return null;
    }

    public Integer getRole(String token){
        Object r = validateToken(token).getBody().get("role");
        if(r instanceof Integer) return (Integer) r;
        return null;
    }
}
