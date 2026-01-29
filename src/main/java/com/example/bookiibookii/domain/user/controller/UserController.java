package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.exception.code.UserSuccessCode;
import com.example.bookiibookii.domain.user.service.UserService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Validated
@RestController
@RequiredArgsConstructor
public class UserController implements UserControllerDocs{
    private final UserService userService;

    // 닉네임 검증
    @Override
    @PostMapping("/api/users/name-validation")
    public ApiResponse<Map<String, Boolean>> validateNickname(
            @NotNull @RequestParam String nickname
    ) {
        boolean isAvailable = userService.isNicknameAvailable(nickname);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, Map.of("isAvailable", isAvailable));
    }

    // User 온보딩 설정
    @Override
    @PostMapping("/api/onboarding")
    public ApiResponse<Void> createUserOnboarding(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody UserRequestDTO.OnboardingReqDTO request
    ) {
        userService.createUserOnboarding(user.getId(), request);
        return ApiResponse.onSuccess(UserSuccessCode.ONBOARDING_SUCCESS, null);
    }

    // MyPage 조회
    @Override
    @GetMapping("/api/mypage")
    public ApiResponse<UserResponseDTO.MypageDTO> getMypage(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        UserResponseDTO.MypageDTO result = userService.getMypageInfo(user.getId());
        return ApiResponse.onSuccess(UserSuccessCode.MYPAGE_SUCCESS, result);
    }

}
