package vn.hoadon.security;

import org.junit.jupiter.api.Test;
import vn.hoadon.entity.UserEntity;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    @Test
    void rememberTokenUsesLongerExpirationAndClaim() {
        JwtUtil jwtUtil = new JwtUtil();
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setCompanyId(1L);
        user.setUsername("demo");
        user.setRole(UserRoles.COMPANY_MANAGER);

        String normalToken = jwtUtil.generateToken(user, "normal-session", false);
        String rememberToken = jwtUtil.generateToken(user, "remember-session", true);

        long normalHours = Duration.between(LocalDateTime.now(), jwtUtil.getExpiration(normalToken)).toHours();
        long rememberDays = Duration.between(LocalDateTime.now(), jwtUtil.getExpiration(rememberToken)).toDays();

        assertThat(normalHours).isBetween(3L, 4L);
        assertThat(rememberDays).isGreaterThanOrEqualTo(29L);
        assertThat(jwtUtil.parseClaims(normalToken).get("remember", Boolean.class)).isFalse();
        assertThat(jwtUtil.parseClaims(rememberToken).get("remember", Boolean.class)).isTrue();
    }
}
