package com.example.bookiibookii.global.auth.jwt;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

// JWT 생성, 검증, Authentication 객체 생성
@Component
public class JwtProvider {
    private final SecretKey secretKey; // JWT 서명에 사용할 비밀키
    private final Duration accessTokenExpireTime;
    private final Duration refreshTokenExpireTime;

    public JwtProvider(
            @Value("${jwt.token.secret-key}") String secretKey,
            @Value("${jwt.token.expiration.access}") Long accessTokenExpireTime,
            @Value("${jwt.token.expiration.refresh}") Long refreshTokenExpireTime
    ) {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("jwt.token.secret-key must be at least 32 bytes for HS256");
        }
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenExpireTime = Duration.ofMillis(accessTokenExpireTime);
        this.refreshTokenExpireTime = Duration.ofMillis(refreshTokenExpireTime);
    }

    // Access Token 생성
    public String createAccessToken(Long userId, String role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpireTime.toMillis());

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("type", "access")
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // Refresh Token 생성
    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpireTime.toMillis());

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    private JwtParser jwtParser() {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build();
    }

    // JWT 유효성 검증
    public boolean validateToken(String token) {
        try {
            jwtParser().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // JWT Claims 추출
    public Claims parseClaims(String token) {
        return jwtParser().parseClaimsJws(token)
                .getBody();
    }

    public Long getUserId(String token) {
        Claims claims = parseClaims(token);
        return Long.valueOf(claims.getSubject());
    }

    public String getRole(String token) {
        Claims claims = parseClaims(token);
        return claims.get("role", String.class);
    }

    // AuthService에서 Redis 저장 시 만료 시간을 알기 위해 필요
    public long getRefreshTokenExpireTime() {
        return refreshTokenExpireTime.toMillis();
    }

    // 토큰의 남은 유효 시간 계산 (밀리초 반환)
    public long getRemainingTime(String token) {
        Date expiration = parseClaims(token).getExpiration();
        long now = new Date().getTime();
        return expiration.getTime() - now;
    }

    // 만료 예외를 무시하고 Claims를 가져오는 로직
    public Long getUserIdIgnoreExpiration(String token) {
        try {
            return Long.valueOf(parseClaims(token).getSubject());
        } catch (ExpiredJwtException e) {
            return Long.valueOf(e.getClaims().getSubject());
        } catch (Exception e) {
            throw new JwtException("유효하지 않은 토큰입니다.");
        }
    }
}
