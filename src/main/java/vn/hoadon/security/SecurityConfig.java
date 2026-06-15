package vn.hoadon.security;

import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.*;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtUtil jwtUtil,
            vn.hoadon.services.UserService userService,
            vn.hoadon.repositories.CompanyRepository companyRepository) {
        return new JwtAuthenticationFilter(jwtUtil, userService, companyRepository);
    }

    @Bean
    public SecurityFilterChain filterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtFilter) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm ->
                        sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                "/uploads/**",
                                "/v1/auth/**",
                                "/v1/file/**",
                                "/h2-console/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/register-invoices/*/download-xml").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/register-invoices/*/xml").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/invoices/*/view").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/invoices/*/download-pdf").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/invoices/*/download-xml").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/invoices/*/xml").permitAll()
                        .requestMatchers(HttpMethod.POST, "/v1/invoice-packages/momo/ipn").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/invoice-packages/momo/return").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/invoice-packages/vnpay/ipn").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v1/invoice-packages/vnpay/return").permitAll()

                        .requestMatchers("/v1/administrator/**").authenticated()
                        .requestMatchers("/v1/setting/**").authenticated()
                        .requestMatchers("/v1/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        http.headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:8080"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
