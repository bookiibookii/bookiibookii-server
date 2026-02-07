package com.example.bookiibookii.global.auth.jwt;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.Status;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.global.auth.CustomUserDetails;
import com.example.bookiibookii.global.auth.exception.AuthException;
import com.example.bookiibookii.global.auth.exception.code.AuthErrorCode;
import com.example.bookiibookii.global.util.RedisUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtTokenResolver tokenResolver; // JWT 토큰 추출
    private final JwtProvider jwtProvider; // JWT 검증
    private final UserRepository userRepository;
    private final RedisUtil redisUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = tokenResolver.resolve(request);
        String path = request.getRequestURI();

        if (token != null && !path.equals("/api/auth/refresh")) {
            try {
                // 블랙리스트 확인
                if (redisUtil.hasKey("BL:" + token)) {
                    // 로그아웃된 토큰이므로 예외 발생 -> catch 블록으로 이동
                    throw new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN);
                }
                Claims claims = jwtProvider.parseClaims(token);

                Long userId = Long.valueOf(claims.getSubject());
                String type = claims.get("type", String.class);

                if(!"access".equals(type))
                    throw new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN);

                // 블랙리스트 통과 후에만 DB 조회 (성능 최적화)
                User user = userRepository.findByIdIncludingWithdrawn(userId)
                        .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

                if (user.getStatus() == Status.WITHDRAWN) {
                    throw new UserException(UserErrorCode.USER_WITHDRAWN);
                }
                CustomUserDetails userDetails = new CustomUserDetails(user);

                // Authentication 생성
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                auth.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (ExpiredJwtException e) { // 만료된 경우
                sendErrorResponse(response, AuthErrorCode.EXPIRED_ACCESS_TOKEN);
                return;
            } catch (JwtException | AuthException e) { // 그 외 잘못된 토큰
                sendErrorResponse(response, AuthErrorCode.INVALID_ACCESS_TOKEN);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendErrorResponse(HttpServletResponse response, AuthErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        String json = String.format(
                "{\"code\": \"%s\", \"message\": \"%s\"}",
                errorCode.getCode(),
                errorCode.getMessage()
        );

        response.getWriter().write(json);
    }
}
