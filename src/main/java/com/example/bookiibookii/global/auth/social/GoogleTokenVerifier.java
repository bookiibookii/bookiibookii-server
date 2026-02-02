package com.example.bookiibookii.global.auth.social;

import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.global.auth.exception.AuthException;
import com.example.bookiibookii.global.auth.exception.code.AuthErrorCode;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class GoogleTokenVerifier implements SocialTokenVerifier {

    @Value("${oauth.google.client-id}")
    private String googleClientId;
    // 매번 생성하지 않도록 멤버 변수로 선언
    private GoogleIdTokenVerifier verifier;

    // 의존성 주입 후 초기화 시점에 Verifier 1회 생성
    @PostConstruct
    private void init() {
        this.verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance() // 최신 표준 사용
        )
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
        try {
            // 미리 생성된 verifier 재사용
            GoogleIdToken idToken = verifier.verify(token);

            if (idToken == null) {
                throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
            }

            GoogleIdToken.Payload payload = idToken.getPayload();

            return new SocialUserInfo(
                    payload.getSubject() // Google 고유 userId (Sub)
            );

        } catch (GeneralSecurityException | IOException e) {
            // 검증 과정에서의 기술적 오류 처리
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }
}
