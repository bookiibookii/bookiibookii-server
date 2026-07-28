package com.example.bookiibookii.global.auth.social;

import com.example.bookiibookii.global.auth.exception.AuthException;
import com.example.bookiibookii.global.auth.exception.code.AuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Base64;
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
     * authorizationCode를 Apple refresh_token으로 교환.
     * 응답의 id_token.sub가 identityToken에서 확인한 expectedSub와 일치하는지 검증하여
     * 서로 다른 Apple 유저의 authorizationCode가 혼입되는 것을 방지한다.
     */
    public String exchangeAuthCode(String authorizationCode, String expectedSub) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", bundleId);
            params.add("client_secret", clientSecretGenerator.generate());
            params.add("code", authorizationCode);
            params.add("grant_type", "authorization_code");

            Map<?, ?> response = restTemplate.postForObject(
                    TOKEN_URL, new HttpEntity<>(params, headers), Map.class);
            if (response == null || !response.containsKey("refresh_token")) {
                throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
            }

            String idToken = (String) response.get("id_token");
            String responseSub = extractSubFromIdToken(idToken);
            if (!expectedSub.equals(responseSub)) {
                log.warn("Apple authorizationCode sub 불일치 — identityToken sub: {}, authorizationCode sub: {}",
                        expectedSub, responseSub);
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

    private String extractSubFromIdToken(String idToken) {
        try {
            String payload = idToken.split("\\.")[1];
            byte[] decoded = Base64.getUrlDecoder().decode(payload);
            Map<?, ?> claims = new ObjectMapper().readValue(decoded, Map.class);
            return (String) claims.get("sub");
        } catch (Exception e) {
            log.error("Apple id_token payload 파싱 실패", e);
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }

    /**
     * Apple refresh_token을 revoke. 성공 여부를 반환한다.
     */
    public boolean revokeToken(String refreshToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", bundleId);
            params.add("client_secret", clientSecretGenerator.generate());
            params.add("token", refreshToken);
            params.add("token_type_hint", "refresh_token");

            restTemplate.postForObject(REVOKE_URL, new HttpEntity<>(params, headers), String.class);
            log.info("Apple token revoke 완료");
            return true;
        } catch (Exception e) {
            log.error("Apple token revoke 실패", e);
            return false;
        }
    }
}
