package vn.hoadon.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.core.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.util.StringUtils;
import java.io.IOException;
import vn.hoadon.services.UserService;
import vn.hoadon.entity.UserEntity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

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

        try {
            String header = request.getHeader("Authorization");

            if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                String username = jwtUtil.getUsername(token);
                Optional<UserEntity> userOpt = userService.findByUsername(username);

                if (userOpt.isPresent()) {
                    UserEntity user = userOpt.get();

                    if (user.getStatus() != null && user.getStatus() == 1) {

                        String roleName = mapRole(user.getRole());
                        List<SimpleGrantedAuthority> authorities =
                                List.of(new SimpleGrantedAuthority("ROLE_" + roleName));

                        Authentication authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user, // principal
                                        null,
                                        authorities
                                );

                        SecurityContextHolder.getContext()
                                .setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }

    private String mapRole(Integer role) {
        return switch (role) {
            case 1 -> "ADMIN";
            case 2 -> "USER";
            default -> "USER";
        };
    }
}
