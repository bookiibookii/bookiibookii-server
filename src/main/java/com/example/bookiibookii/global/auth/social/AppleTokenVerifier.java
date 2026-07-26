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
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleTokenVerifier implements SocialTokenVerifier {

    private static final String APPLE_JWKS_URL = "https://appleid.apple.com/auth/keys";
    private static final String APPLE_ISSUER = "https://appleid.apple.com";
    private static final long CACHE_TTL_MILLIS = TimeUnit.HOURS.toMillis(24);
    private static final long MISS_COOLDOWN_MILLIS = TimeUnit.SECONDS.toMillis(30);
    private static final long FAILURE_COOLDOWN_MILLIS = TimeUnit.SECONDS.toMillis(10);

    @Value("${oauth.apple.bundle-id}")
    private String bundleId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private volatile Map<String, RSAPublicKey> keyCache = Map.of();
    private volatile long lastRefreshedAt = 0;
    private volatile long lastFailedAt = 0;
    private final ReentrantLock refreshLock = new ReentrantLock();

    @PostConstruct
    private void init() {
        try {
            singleFlightRefresh();
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
        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        Map<?, ?> header = objectMapper.readValue(headerJson, Map.class);
        Object kid = header.get("kid");
        if (kid == null) {
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
        return kid.toString();
    }

    private RSAPublicKey resolvePublicKey(String kid) {
        // TTL 만료 시 캐시 전체 갱신 (회수된 키 제거 목적)
        if (isCacheExpired()) {
            singleFlightRefresh();
        }

        RSAPublicKey key = keyCache.get(kid);
        if (key != null) return key;

        // 미등록 kid: 쿨다운이 지난 경우에만 JWKS 재조회 (DoS 방어)
        if (System.currentTimeMillis() - lastRefreshedAt > MISS_COOLDOWN_MILLIS) {
            singleFlightRefresh();
            key = keyCache.get(kid);
        }

        if (key == null) throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        return key;
    }

    // 락을 선점한 스레드만 실제 HTTP 호출, 대기 스레드는 재확인 후 스킵
    private void singleFlightRefresh() {
        refreshLock.lock();
        try {
            long now = System.currentTimeMillis();
            if (now - lastRefreshedAt <= MISS_COOLDOWN_MILLIS) {
                return; // 다른 스레드가 이미 갱신 완료
            }
            if (now - lastFailedAt < FAILURE_COOLDOWN_MILLIS) {
                return; // 최근 JWKS 호출 실패 — backoff 중
            }
            refreshKeys();
        } finally {
            refreshLock.unlock();
        }
    }

    private boolean isCacheExpired() {
        return System.currentTimeMillis() - lastRefreshedAt > CACHE_TTL_MILLIS;
    }

    @SuppressWarnings("unchecked")
    private void refreshKeys() {
        try {
            Map<String, Object> response = restTemplate.getForObject(APPLE_JWKS_URL, Map.class);
            if (response == null) {
                throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
            }
            List<Map<String, String>> keys = (List<Map<String, String>>) response.get("keys");
            Map<String, RSAPublicKey> newCache = new HashMap<>();
            for (Map<String, String> jwk : keys) {
                String kid = jwk.get("kid");
                RSAPublicKey publicKey = buildRsaPublicKey(jwk.get("n"), jwk.get("e"));
                newCache.put(kid, publicKey);
            }
            keyCache = Map.copyOf(newCache);
            lastRefreshedAt = System.currentTimeMillis();
        } catch (AuthException e) {
            lastFailedAt = System.currentTimeMillis();
            throw e;
        } catch (Exception e) {
            lastFailedAt = System.currentTimeMillis();
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
