package com.example.bookiibookii.global.auth.social;

import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.global.auth.exception.AuthException;
import com.example.bookiibookii.global.auth.exception.code.AuthErrorCode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier implements SocialTokenVerifier {

    @Value("${oauth.google.client-id}")
    private String googleClientId;

    // Google ID Token 검증
    private GoogleIdTokenVerifier getVerifier() {
        return new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                JacksonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
    }

    @Override
    public boolean supports(SocialType socialType) {
        return socialType == SocialType.GOOGLE;
    }

    // Google ID Token 검증 및 사용자 정보 추출
    @Override
    public SocialUserInfo verify(String token) {

        GoogleIdToken idToken;

        try {
            idToken = getVerifier().verify(token);
        } catch (Exception e) {
            // 네트워크 오류, 토큰 포맷 오류 등
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }

        // 검증 실패 (위조, 만료, aud 불일치 등)
        if (idToken == null) {
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }

        // 토큰 payload (JWT Claims)
        GoogleIdToken.Payload payload = idToken.getPayload();

        return new SocialUserInfo(
                payload.getSubject()// Google 고유 userId
        );
    }
}
