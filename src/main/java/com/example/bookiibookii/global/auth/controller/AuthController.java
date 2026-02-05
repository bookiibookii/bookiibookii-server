package com.example.bookiibookii.global.auth.controller;

import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import com.example.bookiibookii.global.auth.dto.req.AuthRequestDTO;
import com.example.bookiibookii.global.auth.dto.res.AuthResponseDTO;
import com.example.bookiibookii.global.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController implements AuthControllerDocs{
    private final AuthService authService;

    @Override
    @PostMapping("/login")
    public ApiResponse<AuthResponseDTO.TokenResponse> socialLogin(
            @RequestBody AuthRequestDTO request
    ) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                authService.socialLogin(request.getSocialType(),request.getToken()));
    }

    // 토큰 재발급
    @Override
    @PostMapping("/refresh")
    public ApiResponse<AuthResponseDTO.TokenResponse> refresh(
            @RequestParam String requestRefreshToken,
            HttpServletRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                authService.refresh(requestRefreshToken, request));
    }

    // 로그아웃
    @Override
    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request) {
        authService.logout(request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    // 회원탈퇴
    @DeleteMapping("/withdraw")
    public ApiResponse<Void> withdraw(HttpServletRequest request) {
        authService.withdraw(request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }
}
