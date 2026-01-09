package com.example.bookiibookii.global.auth.service;

import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.domain.user.service.UserService;
import com.example.bookiibookii.global.auth.dto.AuthResDTO;
import com.example.bookiibookii.global.auth.entity.RefreshToken;
import com.example.bookiibookii.global.auth.exception.code.AuthErrorCode;
import com.example.bookiibookii.global.auth.exception.AuthException;
import com.example.bookiibookii.global.auth.jwt.JwtProvider;
import com.example.bookiibookii.global.auth.jwt.JwtTokenResolver;
import com.example.bookiibookii.global.auth.social.SocialTokenVerifier;
import com.example.bookiibookii.global.auth.social.SocialUserInfo;
import com.example.bookiibookii.global.auth.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserService userService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenResolver jwtTokenResolver;
    private final JwtProvider jwtProvider;

    // social Type기준으로 분기 처리
    private final List<SocialTokenVerifier> tokenVerifiers;

    // 소셜 로그인
    public AuthResDTO.TokenResponse socialLogin(String socialType, String token) {

        SocialType social = SocialType.valueOf(socialType);

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

        // RefreshToken 저장 (기존 토큰 존재하면 갱신)
        refreshTokenRepository.findById(userId)
                .ifPresentOrElse(
                        saved -> saved.update(refreshToken),
                        () -> refreshTokenRepository.save(
                                RefreshToken.of(userId, refreshToken)
                        )
                );
        
        return AuthResDTO.TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(userId)
                .build();
    }

    
    // Access Token 재발급
    public AuthResDTO.TokenResponse refresh(HttpServletRequest request) {

        String refreshToken = jwtTokenResolver.resolve(request);

        if (refreshToken == null) {
            throw new AuthException(AuthErrorCode.NOT_FOUND_REFRESH_TOKEN);
        }

        // refresh token 유효성 검증
        jwtProvider.validateToken(refreshToken);

        Long userId = jwtProvider.getUserId(refreshToken);
        String role = jwtProvider.getRole(refreshToken);

        RefreshToken savedToken = refreshTokenRepository.findByUserId(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));

        if (!savedToken.getToken().equals(refreshToken)) {
            throw new AuthException(AuthErrorCode.NOT_FOUND);
        }

        // 새 토큰 발급
        String newAccessToken = jwtProvider.createAccessToken(userId, role);
        String newRefreshToken = jwtProvider.createRefreshToken(userId);

        savedToken.update(newRefreshToken);

        return AuthResDTO.TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .userId(userId)
                .build();
    }


    // 로그아웃
    public void logout(HttpServletRequest request) {
        String refreshToken = jwtTokenResolver.resolve(request);

        if (refreshToken == null) {
            return; // 로그인 안 되어 있어도 로그아웃은 성공
        }

        Long userId = jwtProvider.getUserId(refreshToken);
        refreshTokenRepository.deleteByUserId(userId);
    }
}
