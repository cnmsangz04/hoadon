package vn.hoadon.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import vn.hoadon.entity.UserEntity;
import vn.hoadon.services.UserService;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtUtil jwtUtil;
    private final UserService userService;

    public JwtAuthenticationFilter(JwtUtil jwtUtil, UserService userService) {
        this.jwtUtil = jwtUtil;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String method = request.getMethod();
        String header = request.getHeader("Authorization");

        log.debug("Request URI: {}, Method: {}, Authorization Header Present: {}", uri, method, StringUtils.hasText(header));

        if (!StringUtils.hasText(header)) {
            log.debug("No Authorization header found, skipping JWT filter");
            chain.doFilter(request, response);
            return;
        }

        if (!header.startsWith("Bearer ")) {
            log.warn("Invalid Authorization header format: {}", header);
            unauthorized(response, "INVALID_HEADER",
                    "Authorization header must start with Bearer");
            return;
        }

        String token = header.substring(7);
        log.debug("Extracted JWT token: {}", token);

        try {
            String username = jwtUtil.getUsername(token);
            log.debug("Decoded username from JWT: {}", username);

            Optional<UserEntity> userOpt = userService.findByUsername(username);
            if (userOpt.isEmpty()) {
                log.warn("User not found: {}", username);
                unauthorized(response, "USER_NOT_FOUND", "User does not exist");
                return;
            }

            UserEntity user = userOpt.get();
            log.debug("User found: id={}, username={}, status={}", user.getId(), user.getUsername(), user.getStatus());

            if (user.getStatus() == null || user.getStatus() != 1) {
                log.warn("User is disabled or inactive: {}", username);
                unauthorized(response, "USER_DISABLED", "User is inactive or disabled");
                return;
            }

            List<SimpleGrantedAuthority> authorities = mapAuthorities(user.getRole());
            log.debug("Mapped authorities for user {}: {}", username, authorities);

            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    user,
                    null,
                    authorities
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("User {} authenticated successfully", username);

        } catch (ExpiredJwtException e) {
            SecurityContextHolder.clearContext();
            log.warn("JWT token expired: {}", e.getMessage());
            unauthorized(response, "TOKEN_EXPIRED", "JWT token has expired");
            return;

        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
            log.warn("Invalid JWT token: {}", e.getMessage());
            unauthorized(response, "INVALID_TOKEN", "JWT token is invalid");
            return;

        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            log.error("Unexpected authentication error", e);
            unauthorized(response, "AUTH_ERROR", "Authentication error");
            return;
        }

        chain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> mapAuthorities(Integer role) {
        List<SimpleGrantedAuthority> list = new ArrayList<>();

        if (role == null) {
            list.add(new SimpleGrantedAuthority("ROLE_USER"));
            return list;
        }

        switch (role) {
            case 0 -> {
                list.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                list.add(new SimpleGrantedAuthority("ROLE_USER"));
            }
            case 1 -> list.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            case 2 -> list.add(new SimpleGrantedAuthority("ROLE_USER"));
            default -> list.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return list;
    }

    private void unauthorized(HttpServletResponse response,
                              String error,
                              String message) throws IOException {

        log.debug("Responding unauthorized: error={}, message={}", error, message);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("""
            {
              "error": "%s",
              "message": "%s"
            }
            """.formatted(error, message));
    }

    private String mapRole(Integer role) {
        return switch (role) {
            case 0, 1 -> "ADMIN";
            case 2 -> "USER";
            default -> "USER";
        };
    }
}