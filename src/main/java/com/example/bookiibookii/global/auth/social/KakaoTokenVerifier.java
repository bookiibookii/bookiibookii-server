package com.example.bookiibookii.global.auth.social;

import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.global.auth.exception.code.AuthErrorCode;
import com.example.bookiibookii.global.auth.exception.AuthException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoTokenVerifier implements SocialTokenVerifier {

    private final RestTemplate restTemplate; // 토큰 검증을 위해 소셜 서버에 HTTP 요청을 보냄

    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Override
    public boolean supports(SocialType socialType) {
        return socialType == SocialType.KAKAO;
    }

    @Override
    @SuppressWarnings("unchecked")
    public SocialUserInfo verify(String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<?> request = new HttpEntity<>(headers);

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(
                    KAKAO_USER_INFO_URL,
                    HttpMethod.GET,
                    request,
                    Map.class
            );
        } catch (Exception e) {
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }

        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }

        String socialId = String.valueOf(body.get("id"));
        return new SocialUserInfo(socialId);
    }
}
