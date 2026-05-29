package com.example.bookiibookii.global.security;

import com.example.bookiibookii.global.auth.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    private static final String[] PERMIT_URLS = {
            "/",
            "/health",
            "/error",

            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger/login/**",
            "/swagger-resources/**",
            "/swagger-ui.html",

            "/api/auth/login",
            "/api/auth/refresh",
            "/kakao/callback",
            "/google/callback"
    };

    // Security Filter Chain 설정
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능
                        .requestMatchers(PERMIT_URLS).permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // 관리자 전용 API
                        .anyRequest().authenticated()
                )
                .addFilterBefore( // JWT 인증 필터 등록 (UsernamePasswordAuthenticationFilter 이전에 실행)
                        jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 오리진 명시 (보안을 위해 * 대신 실제 주소만 입력)
        configuration.setAllowedOrigins(List.of(
                "https://bookii-admin.vercel.app",
                "https://admin.bookiibookii.com"
        ));

        // 허용할 HTTP 메서드 (필요한 것만 명시)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH"));

        // 허용할 헤더 (JWT 인증을 위한 Authorization 헤더 포함)
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Cache-Control"));

        configuration.setAllowCredentials(true); // 자격 증명 허용

        // 브라우저가 CORS 정보를 캐싱하는 시간 설정
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
