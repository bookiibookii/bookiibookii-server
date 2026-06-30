package com.example.bookiibookii.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@Profile({"v1", "prod"})
public class SwaggerSecurityConfig {

    @Value("${swagger.auth.username}")
    private String username;

    @Value("${swagger.auth.password}")
    private String password;

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        UserDetails swaggerUser = User.builder()
                .username(username)
                .password(encoder.encode(password))
                .roles("SWAGGER")
                .build();

        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(new InMemoryUserDetailsManager(swaggerUser));
        authProvider.setPasswordEncoder(encoder);

        http
                .securityMatcher("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/swagger/login/**", "/swagger-resources/**")
                .authenticationProvider(authProvider)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
