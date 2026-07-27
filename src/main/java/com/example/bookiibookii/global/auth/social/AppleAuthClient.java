package com.example.bookiibookii.global.auth.social;

import com.example.bookiibookii.global.auth.exception.AuthException;
import com.example.bookiibookii.global.auth.exception.code.AuthErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Apple 서버에 HTTP 요청을 보내는 클라이언트.
 * - exchangeAuthCode(): 로그인 시 authorizationCode → refresh_token 교환
 * - revokeToken(): 탈퇴 시 refresh_token revoke (App Store 심사 지침)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AppleAuthClient {

    private static final String TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String REVOKE_URL = "https://appleid.apple.com/auth/revoke";

    @Value("${oauth.apple.bundle-id}")
    private String bundleId;

    private final RestTemplate restTemplate;
    private final AppleClientSecretGenerator clientSecretGenerator;

    /**
     * authorizationCode를 Apple refresh_token으로 교환
     */
    public String exchangeAuthCode(String authorizationCode) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", bundleId);
        params.add("client_secret", clientSecretGenerator.generate());
        params.add("code", authorizationCode);
        params.add("grant_type", "authorization_code");

        try {
            Map<?, ?> response = restTemplate.postForObject(
                    TOKEN_URL, new HttpEntity<>(params, headers), Map.class);
            if (response == null || !response.containsKey("refresh_token")) {
                throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
            }
            return (String) response.get("refresh_token");
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Apple authorizationCode 교환 실패", e);
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }

    /**
     * Apple refresh_token을 revoke
     */
    public void revokeToken(String refreshToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", bundleId);
        params.add("client_secret", clientSecretGenerator.generate());
        params.add("token", refreshToken);
        params.add("token_type_hint", "refresh_token");

        try {
            restTemplate.postForObject(REVOKE_URL, new HttpEntity<>(params, headers), String.class);
            log.info("Apple token revoke 완료");
        } catch (Exception e) {
            log.error("Apple token revoke 실패 — 탈퇴는 계속 진행", e);
        }
    }
}
