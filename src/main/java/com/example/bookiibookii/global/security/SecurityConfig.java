package com.example.bookiibookii.global.security;

import com.example.bookiibookii.global.auth.jwt.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    private static final String[] PERMIT_URLS = {
            "/",
            "/health",

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
                );

        return http.build();
    }
}
