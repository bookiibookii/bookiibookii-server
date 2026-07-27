package com.example.bookiibookii.global.auth.service;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.domain.user.enums.Status;
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
import com.example.bookiibookii.global.auth.social.AppleAuthClient;
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
    private final AppleAuthClient appleAuthClient;

    // 소셜 로그인
    public AuthResponseDTO.LoginResponse socialLogin(AuthRequestDTO request) {

        final SocialType social;
        try {
            social = SocialType.valueOf(request.getSocialType());
        } catch(IllegalArgumentException | NullPointerException e) {
            throw new AuthException(AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE);
        }

        // socialType에 맞는 verifier 선택
        SocialTokenVerifier verifier = tokenVerifiers.stream()
                .filter(v -> v.supports(social))
                .findFirst()
                .orElseThrow(() -> new AuthException(AuthErrorCode.UNSUPPORTED_SOCIAL_TYPE));

        // 소셜 토큰 검증 → 사용자 정보 획득
        SocialUserInfo socialUserInfo = verifier.verify(request.getToken());

        // 유저 조회 or 생성
        User user = userService.findOrCreateSocialUser(socialUserInfo, social);

        // Apple: authorizationCode → refresh_token 교환 후 저장 (탈퇴 시 revoke에 사용)
        // 교환 실패 시 로그인 자체를 중단 — revoke 불가 상태로 계정을 만들지 않기 위해
        if (social == SocialType.APPLE && request.getAuthorizationCode() != null) {
            String appleRefreshToken = appleAuthClient.exchangeAuthCode(request.getAuthorizationCode());
            user.updateAppleRefreshToken(appleRefreshToken);
        }

        return issueLoginToken(user);
    }

    public AuthResponseDTO.LoginResponse issueLoginToken(User user) {
        Long userId = user.getId();
        String role = user.getRole().name();

        // JWT 발급
        String accessToken = jwtProvider.createAccessToken(userId, role);
        String refreshToken = jwtProvider.createRefreshToken(userId);

        // Redis에 Refresh Token 저장
        // Key: "RT:{userId}", Value: refreshToken
        // Expiration: application.yml에 설정된 시간 (밀리초 -> 분 변환 필요)
        int rtExpirationMinutes = (int) Math.ceil(jwtProvider.getRefreshTokenExpireTime() / 1000.0 / 60);
        String rtKey = "RT:" + userId;
        redisUtil.set(rtKey, refreshToken, rtExpirationMinutes);

        return AuthResponseDTO.LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .onboardingStatus(user.getOnboardingStatus())
                .role(user.getRole())
                .build();
    }


    // Access Token 재발급
    public AuthResponseDTO.TokenResponse refresh(AuthRequestDTO.RefreshRequest requestDTO,
                                                 HttpServletRequest request) {
        String accessToken = jwtTokenResolver.resolve(request);
        String requestRT = requestDTO.refreshToken();

        if (accessToken == null || requestRT == null) {
            throw new AuthException(AuthErrorCode.NOT_FOUND_ACCESS_TOKEN);
        }

        // RT 자체 유효성 선검증 (서명, 만료여부)
        if (!jwtProvider.validateToken(requestRT)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // AT에서 유저 정보 추출 (만료 무시)
        Long userId = jwtProvider.getUserIdIgnoreExpiration(accessToken);

        // Redis 대조
        String rtKey = "RT:" + userId;
        String savedRT = redisUtil.get(rtKey, String.class);
        if (savedRT == null || !savedRT.equals(requestRT)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 사용자 존재 확인 및 권한 획득
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.NOT_FOUND));

        if (user.getStatus() == Status.WITHDRAWN) {
            throw new AuthException(AuthErrorCode.NOT_FOUND);
        }

        // 신규 토큰 생성 및 Redis 갱신
        String newAT = jwtProvider.createAccessToken(userId, user.getRole().name());
        String newRT = jwtProvider.createRefreshToken(userId);

        int rtExpirationMinutes = (int) Math.ceil(jwtProvider.getRefreshTokenExpireTime() / 1000.0 / 60);
        redisUtil.set(rtKey, newRT, rtExpirationMinutes);

        return AuthResponseDTO.TokenResponse.builder()
                .accessToken(newAT)
                .refreshToken(newRT)
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
        try {
            if (!jwtProvider.validateToken(accessToken)) {
                return; // 유효하지 않으면 블랙리스트 등록도 필요 없음
            }
        } catch (JwtException e) {
            log.debug("로그아웃 토큰 검증 실패: {}", e.getMessage());
            return;
        }

        // Redis 처리 (시스템/인프라 관련 예외 처리)
        try {
            Long userId = jwtProvider.getUserId(accessToken);

            // Refresh Token 삭제
            String rtKey = "RT:" + userId;
            redisUtil.delete(rtKey);

            // Access Token 남은 시간 계산
            long remainingTime = jwtProvider.getRemainingTime(accessToken);

            // Redis에 Blacklist 등록
            if (remainingTime > 0) {
                redisUtil.setBlackList("BL:" + accessToken, "logout", remainingTime);
            }

        } catch (Exception e) {
            log.error("로그아웃 처리 중 에러 발생.", e);
        }
    }

}
