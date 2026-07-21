package com.example.bookiibookii.global.auth.social;

import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.global.auth.exception.AuthException;
import com.example.bookiibookii.global.auth.exception.code.AuthErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleTokenVerifier implements SocialTokenVerifier {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";

    @Value("${oauth.apple.bundle-id}")
    private String bundleId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // kid → RSAPublicKey 캐시 (Apple 공개키는 자주 바뀌지 않으므로 메모리 캐시)
    private final Map<String, RSAPublicKey> keyCache = new ConcurrentHashMap<>();

    @PostConstruct
    private void init() {
        try {
            refreshKeys();
        } catch (Exception e) {
            log.warn("Apple JWKS 사전 로드 실패 — 첫 요청 시 재시도합니다.", e);
        }
    }

    @Override
    public boolean supports(SocialType socialType) {
        return socialType == SocialType.APPLE;
    }

    @Override
    public SocialUserInfo verify(String token) {
        try {
            String kid = extractKid(token);
            RSAPublicKey publicKey = resolvePublicKey(kid);

            Claims claims = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            validateClaims(claims);

            return new SocialUserInfo(claims.getSubject());

        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Apple 토큰 검증 실패", e);
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }

    private String extractKid(String token) throws Exception {
        String[] parts = token.split("\\.");
        if (parts.length < 2) {
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        Map<?, ?> header = objectMapper.readValue(headerJson, Map.class);
        Object kid = header.get("kid");
        if (kid == null) {
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
        return kid.toString();
    }

    // 캐시에서 공개키 조회, 없으면 Apple JWKS 재조회 후 재시도
    private RSAPublicKey resolvePublicKey(String kid) {
        RSAPublicKey cached = keyCache.get(kid);
        if (cached != null) {
            return cached;
        }
        refreshKeys();
        RSAPublicKey key = keyCache.get(kid);
        if (key == null) {
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
        return key;
    }

    @SuppressWarnings("unchecked")
    private void refreshKeys() {
        try {
            Map<String, Object> response = restTemplate.getForObject(APPLE_JWKS_URL, Map.class);
            if (response == null) {
                throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
            }
            List<Map<String, String>> keys = (List<Map<String, String>>) response.get("keys");
            keyCache.clear();
            for (Map<String, String> jwk : keys) {
                String kid = jwk.get("kid");
                RSAPublicKey publicKey = buildRsaPublicKey(jwk.get("n"), jwk.get("e"));
                keyCache.put(kid, publicKey);
            }
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Apple JWKS 조회 실패", e);
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }

    private RSAPublicKey buildRsaPublicKey(String n, String e) throws Exception {
        BigInteger modulus = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        BigInteger exponent = new BigInteger(1, Base64.getUrlDecoder().decode(e));
        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
    }

    private void validateClaims(Claims claims) {
        if (!APPLE_ISSUER.equals(claims.getIssuer())) {
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
        // JJWT 0.12.x audience는 Set<String>으로 반환
        if (!claims.getAudience().contains(bundleId)) {
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }
}
