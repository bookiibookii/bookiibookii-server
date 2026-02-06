package com.example.bookiibookii.global.auth.controller;

import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import com.example.bookiibookii.global.auth.dto.res.AuthResponseDTO;
import com.example.bookiibookii.global.auth.exception.AuthException;
import com.example.bookiibookii.global.auth.exception.code.AuthErrorCode;
import com.example.bookiibookii.global.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * SDK 연동 전 백엔드 테스트용 컨트롤러
 * - 실제 배포 시 삭제
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OAuthTestController {
    private final AuthService authService;
    private final RestTemplate restTemplate;

    @Value("${oauth.kakao.client-id}")
    private String KAKAO_REST_API_KEY;

    @Value("${oauth.kakao.redirect-uri}")
    private String KAKAO_REDIRECT_URI;

    private static final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";

    @GetMapping("/kakao/callback")
    public ApiResponse<AuthResponseDTO.LoginResponse> kakaoCallback(@RequestParam String code) {
        // 테스트용 코드 → 실제 AuthService 호출 가능
        // 여기선 code를 토큰처럼 사용하여 AuthService를 호출하도록 시뮬레이션
        String accessToken = getKakaoAccessToken(code);
        log.info("AccessToken: {}", accessToken);
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                authService.socialLogin("KAKAO", accessToken));
    }

    @GetMapping("/google/callback")
    public AuthResponseDTO.LoginResponse googleCallback(@RequestParam String code) {
        return authService.socialLogin("GOOGLE", code);
    }

    private String getKakaoAccessToken(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", KAKAO_REST_API_KEY);
        params.add("redirect_uri", KAKAO_REDIRECT_URI);
        params.add("code", code);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    KAKAO_TOKEN_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            log.info("Token response status: {}", response.getStatusCode());
            log.info("Token response body: {}", response.getBody());

            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("access_token")) {
                throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
            }

            return (String) body.get("access_token");

        } catch (Exception e) {
            log.error("Failed to get Kakao Access Token", e);
            throw new AuthException(AuthErrorCode.INVALID_SOCIAL_TOKEN);
        }
    }
}
