package com.example.bookiibookii.global.auth.social;

import io.jsonwebtoken.Jwts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

/**
 * Apple API 호출 시 필요한 client_secret을 생성 (Apple은 고정 문자열 대신 .p8 개인키로 ES256 서명한 JWT를 client_secret으로 요구하기 때문)
 * AppleAuthClient가 /auth/token, /auth/revoke 호출할 때마다 사용
 */
@Slf4j
@Component
public class AppleClientSecretGenerator {

    private static final String APPLE_AUDIENCE = "https://appleid.apple.com";
    private static final long EXPIRY_MILLIS = 15_777_000_000L; // 약 6개월

    @Value("${oauth.apple.team-id}")
    private String teamId;

    @Value("${oauth.apple.key-id}")
    private String keyId;

    @Value("${oauth.apple.bundle-id}")
    private String bundleId;

    @Value("${oauth.apple.private-key}")
    private String privateKeyPem;

    public String generate() {
        try {
            ECPrivateKey privateKey = parsePrivateKey(privateKeyPem);
            long now = System.currentTimeMillis();
            return Jwts.builder()
                    .header().add("kid", keyId).and()
                    .issuer(teamId)
                    .issuedAt(new Date(now))
                    .expiration(new Date(now + EXPIRY_MILLIS))
                    .audience().add(APPLE_AUDIENCE).and()
                    .subject(bundleId)
                    .signWith(privateKey, Jwts.SIG.ES256)
                    .compact();
        } catch (Exception e) {
            throw new RuntimeException("Apple client_secret 생성 실패", e);
        }
    }

    private ECPrivateKey parsePrivateKey(String pem) throws Exception {
        String base64 = pem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(base64);
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }
}
