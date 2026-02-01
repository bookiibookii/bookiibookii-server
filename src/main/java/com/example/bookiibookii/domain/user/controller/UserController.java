package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.domain.user.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.exception.code.UserImageSuccessCode;
import com.example.bookiibookii.domain.user.exception.code.UserSuccessCode;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
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
    private static final int PRESIGNED_URL_EXPIRATION_MINUTES = 10;

    private final UserService userService;
    private final UserImageS3Service userImageS3Service;

    // 사용자 이미지 업로드용 Presigned URL 발급
    @Override
    @PostMapping("/api/users/me/image/presigned-url")
    public ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForUserImage(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        PresignedUrlResponseDTO responseDTO = userImageS3Service.generatePresignedPutUrl(user.getId(), PRESIGNED_URL_EXPIRATION_MINUTES);
        return ApiResponse.onSuccess(UserImageSuccessCode.PRESIGNED_URL_ISSUED, responseDTO);
    }

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
    public ApiResponse<UserResponseDTO.UserProfileResDTO> getMypage(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        UserResponseDTO.UserProfileResDTO result = userService.getMypageInfo(user.getId());
        return ApiResponse.onSuccess(UserSuccessCode.MYPAGE_SUCCESS, result);
    }

}
