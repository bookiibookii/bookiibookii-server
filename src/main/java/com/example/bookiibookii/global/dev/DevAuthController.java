package com.example.bookiibookii.global.dev;

import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import com.example.bookiibookii.global.auth.dto.res.AuthResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// todo : 테스트용이므로 나중에 삭제
@RestController
@Profile("local")
@RequestMapping("/api/dev")
@RequiredArgsConstructor
public class DevAuthController {

    private final DevAuthService devAuthService;

    @PostMapping("/login")
    public ApiResponse<AuthResponseDTO.LoginResponse> login(@RequestParam Long userId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                devAuthService.login(userId)
        );
    }
}
