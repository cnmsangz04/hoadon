package vn.hoadon.security;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
// --- IMPORT THÊM CÁC GÓI NÀY ĐỂ XỬ LÝ CORS ---
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(){
        return new JwtAuthenticationFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. THÊM DÒNG NÀY ĐẦU TIÊN: Bật cấu hình CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            .csrf(csrf -> csrf.disable()) // Cách viết mới của Spring Security 6+ (hoặc giữ .csrf().disable() nếu bản cũ)
            .sessionManagement(session -> session.disable()) // Tắt session vì dùng JWT
            
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v1/**","/api/**", "/public/**", "/v3/api-docs/**", "/swagger-ui/**").permitAll()
                // Nếu muốn request OPTIONS (Pre-flight) luôn được qua (đề phòng)
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            )
            
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // 2. BEAN CẤU HÌNH CHI TIẾT CORS (QUAN TRỌNG)
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Cho phép tất cả các nguồn (Vue đang chạy ở port khác)
        configuration.setAllowedOrigins(Arrays.asList("*")); 
        
        // Cho phép đầy đủ các method: QUAN TRỌNG PHẢI CÓ PUT, DELETE, OPTIONS
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD"));
        
        // Cho phép mọi header (như Authorization, Content-Type...)
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        // Nếu dùng credentials (cookie), setAllowedOrigins không được để "*" mà phải chỉ định rõ domain.
        // Nhưng với JWT qua Header thì để "*" thường vẫn ổn.
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }
}