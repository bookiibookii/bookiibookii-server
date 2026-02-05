package com.example.bookiibookii.global.auth.service;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.domain.user.exception.UserException;
import com.example.bookiibookii.domain.user.exception.code.UserErrorCode;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.example.bookiibookii.domain.user.service.UserService;
import com.example.bookiibookii.global.auth.dto.req.AuthRequestDTO;
import com.example.bookiibookii.global.auth.dto.res.AuthResponseDTO;
import com.example.bookiibookii.global.auth.exception.code.AuthErrorCode;
import com.example.bookiibookii.global.auth.exception.AuthException;
import com.example.bookiibookii.global.auth.jwt.JwtProvider;
import com.example.bookiibookii.global.auth.jwt.JwtTokenResolver;
import com.example.bookiibookii.global.auth.social.SocialTokenVerifier;
import com.example.bookiibookii.global.auth.social.SocialUserInfo;
import com.example.bookiibookii.global.util.RedisUtil;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtTokenResolver jwtTokenResolver;
    private final JwtProvider jwtProvider;
    private final RedisUtil redisUtil;

    // social Type기준으로 분기 처리
    private final List<SocialTokenVerifier> tokenVerifiers;

    // 소셜 로그인
    public AuthResponseDTO.TokenResponse socialLogin(String socialType, String token) {

        final SocialType social;
        try {
            social = SocialType.valueOf(socialType);
        } catch(IllegalArgumentException | NullPointerException e) {
            throw new AuthException(AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE);
        }

        // socialType에 맞는 verifier 선택
        SocialTokenVerifier verifier = tokenVerifiers.stream()
                .filter(v -> v.supports(social))
                .findFirst()
                .orElseThrow(() -> new AuthException(AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE));

        // 소셜 토큰 검증 → 사용자 정보 획득
        SocialUserInfo socialUserInfo = verifier.verify(token);

        // 유저 조회 or 생성
        User user = userService.findOrCreateSocialUser(socialUserInfo, social);

        Long userId = user.getId();
        String role = user.getRole().name();

        // JWT 발급
        String accessToken = jwtProvider.createAccessToken(userId, role);
        String refreshToken = jwtProvider.createRefreshToken(userId);

        // Redis에 Refresh Token 저장
        // Key: "RT:{userId}", Value: refreshToken
        // Expiration: application.yml에 설정된 시간 (밀리초 -> 분 변환 필요)
        int rtExpirationMinutes = (int) (jwtProvider.getRefreshTokenExpireTime() / 1000 / 60);
        redisUtil.set("RT:" + userId, refreshToken, rtExpirationMinutes);

        return AuthResponseDTO.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .build();
    }

    
    // Access Token 재발급
    public AuthResponseDTO.TokenResponse refresh(AuthRequestDTO.RefreshRequest requestRefreshToken,
                                                 HttpServletRequest request) {
        String accessToken = jwtTokenResolver.resolve(request);
        if (accessToken == null) {
            throw new AuthException(AuthErrorCode.NOT_FOUND_ACCESS_TOKEN);
        }

        Long userId;
        try {
            userId = jwtProvider.getUserIdIgnoreExpiration(accessToken);
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        // Redis에서 Refresh Token 조회
        String savedRefreshToken = redisUtil.get("RT:" + userId, String.class);

        // Redis에 토큰이 있는지 + 클라이언트가 보낸 것과 일치하는지 + 유효한지
        if (savedRefreshToken == null ||
                !savedRefreshToken.equals(requestRefreshToken.refreshToken()) ||
                !jwtProvider.validateToken(requestRefreshToken.refreshToken())) {

            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.NOT_FOUND));
        String role = user.getRole().name();

        // 새 토큰 발급
        String newAccessToken = jwtProvider.createAccessToken(userId, role);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        // Redis 업데이트 (덮어쓰기)
        int rtExpirationMinutes = (int) Math.ceil(jwtProvider.getRefreshTokenExpireTime() / 1000.0 / 60);
        redisUtil.set("RT:" + userId, newRefreshToken, rtExpirationMinutes);

        return AuthResponseDTO.TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(userId)
                .build();
    }


    // 로그아웃
    public void logout(HttpServletRequest request) {
        String accessToken = jwtTokenResolver.resolve(request);

        if (accessToken == null) {
            return;
        }

        // 토큰 유효성 검증 (JWT 관련 예외 처리)
        // validateToken 내부에서 이미 예외를 잡아서 false를 반환하도록 되어 있다면 try-catch 불필요
        // 하지만 getUserId나 getRemainingTime에서 발생할 수 있는 JwtException을 대비
        try {
            if (!jwtProvider.validateToken(accessToken)) {
                log.debug("로그아웃 요청이 왔으나 유효하지 않은 토큰임: {}", accessToken);
                return; // 유효하지 않으면 블랙리스트 등록도 필요 없음
            }
        } catch (JwtException e) {
            // 파싱 불가능하거나 만료된 토큰은 무시 (또는 Debug 로그)
            log.debug("로그아웃 토큰 검증 실패: {}", e.getMessage());
            return;
        }

        // Redis 처리 (시스템/인프라 관련 예외 처리)
        try {
            Long userId = jwtProvider.getUserId(accessToken);

            // Refresh Token 삭제
            redisUtil.delete("RT:" + userId);

            // Access Token 남은 시간 계산
            long remainingTime = jwtProvider.getRemainingTime(accessToken);

            // Redis에 Blacklist 등록
            if (remainingTime > 0) {
                redisUtil.setBlackList("BL:" + accessToken, "logout", remainingTime);
            }

        } catch (Exception e) {
            log.error("로그아웃 중 Redis 처리 에러 발생. user access token: {}", accessToken, e);
        }
    }

    // 회원탈퇴
    public void withdraw(HttpServletRequest request) {

        String accessToken = jwtTokenResolver.resolve(request);
        if (accessToken == null) {
            throw new AuthException(AuthErrorCode.NOT_FOUND_ACCESS_TOKEN);
        }

        try {
            jwtProvider.validateToken(accessToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }
        Long userId = jwtProvider.getUserId(accessToken);

        User user = userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(UserErrorCode.NOT_FOUND));

        redisUtil.delete("RT:" + userId); // RefreshToken 제거
        user.withdraw();
    }

}
