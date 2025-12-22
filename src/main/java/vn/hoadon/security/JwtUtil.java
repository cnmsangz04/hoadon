package vn.hoadon.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import vn.hoadon.entity.UserEntity;

import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String SECRET =
        "replace-this-with-a-very-long-secret-key-should-be-256-bit";
    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    private final long expirationMs = 1000 * 60 * 60 * 4;

    public String generateToken(UserEntity user) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(user.getUsername())
                .claim("userId", user.getId())
                .claim("role", user.getRole())
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getUsername(String token) {
        return parseClaims(token).getSubject();
    }
}
