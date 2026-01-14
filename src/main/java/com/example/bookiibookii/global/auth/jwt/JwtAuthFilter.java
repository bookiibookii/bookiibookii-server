package com.example.bookiibookii.global.auth.jwt;

import com.example.bookiibookii.domain.user.enums.Status;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtTokenResolver tokenResolver; // JWT 토큰 추출
    private final JwtProvider jwtProvider; // JWT 검증
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = tokenResolver.resolve(request);

        if (token != null) {
            try {
                // ACTIVE 유저만 인증 허용
                Long userId = jwtProvider.getUserId(token);
                userRepository.findByIdAndStatus(userId, Status.ACTIVE)
                        .orElseThrow(() -> new UserException(UserErrorCode.USER_WITHDRAWN));

                Authentication auth = jwtProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | UserException e) {
                request.setAttribute("jwt_exception", e);
            }
        }

        filterChain.doFilter(request, response);
    }
}
