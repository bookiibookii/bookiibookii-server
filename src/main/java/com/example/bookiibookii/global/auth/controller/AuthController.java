package com.example.bookiibookii.global.auth.controller;

import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.auth.dto.AuthReqDTO;
import com.example.bookiibookii.global.auth.dto.AuthResDTO;
import com.example.bookiibookii.global.auth.exception.code.AuthSuccessCode;
import com.example.bookiibookii.global.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthControllerDocs{
    private final AuthService authService;

    @Override
    @PostMapping("/login")
    public AuthResDTO.TokenResponse sociallogin(
            @RequestBody AuthReqDTO request
    ) {
        return authService.socialLogin(
                request.getSocialType(),
                request.getToken()
        );
    }

    // 토큰 재발급
    @Override
    @PostMapping("/refresh")
    public AuthResDTO.TokenResponse refresh(HttpServletRequest request) {
        return authService.refresh(request);
    }

    // 로그아웃
    @Override
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ApiResponse.onSuccess(AuthSuccessCode.LOGOUT_SUCCESS, null);
    }

    // TODO : 회원 탈퇴 (소셜 계정 연결 해제 요청)
}
