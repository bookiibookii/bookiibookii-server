package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.domain.user.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.NicknameStatus;
import com.example.bookiibookii.domain.user.exception.code.UserImageSuccessCode;
import com.example.bookiibookii.domain.user.exception.code.UserSuccessCode;
import com.example.bookiibookii.domain.user.service.UserImageS3Service;
import com.example.bookiibookii.domain.user.service.UserService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    public ApiResponse<UserResponseDTO.NicknameValidationDTO> validateNickname(
            @NotBlank(message = "닉네임은 필수 입력 사항입니다.")
            @Size(max = 10, message = "닉네임은 10자 이하여야 합니다.")
            @RequestParam String nickname
    ) {
        NicknameStatus status = userService.checkNicknameStatus(nickname);
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                UserResponseDTO.NicknameValidationDTO.from(status)
        );
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

    // 스플래시 온보딩 스킵 (온보딩 상태 변경)
    @Override
    @PatchMapping("/api/onboarding-skip")
    public ApiResponse<Void> updateOnboardingStatus(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        userService.updateOnboardingStatus(user.getId());
        return ApiResponse.onSuccess(UserSuccessCode.ONBOARDING_SUCCESS, null);
    }

    // MyPage 조회
    @Override
    @GetMapping("/api/mypage")
    public ApiResponse<UserResponseDTO.UserProfileResDTO> getMypage(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        List<GroupStatus> statuses = List.of(GroupStatus.RECRUITING, GroupStatus.MATCHED);
        UserResponseDTO.UserProfileResDTO result = userService.getProfileInfo(user.getId(), statuses);
        return ApiResponse.onSuccess(UserSuccessCode.GET_MYPAGE_SUCCESS, result);
    }

    // MyPage 정보 수정
    @Override
    @PatchMapping("/api/mypage")
    public ApiResponse<Void> updateMypage(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody UserRequestDTO.MypageReqDTO request
    ) {
        userService.updateMypage(user.getId(), request);
        return ApiResponse.onSuccess(UserSuccessCode.UPDATE_MYPAGE_SUCCESS, null);
    }

    // 타 유저 프로필 조회
    @Override
    @GetMapping("/api/profiles/{nickname}")
    public ApiResponse<UserResponseDTO.UserProfileResDTO> getOtherProfile(
            @PathVariable("nickname") String nickname
    ) {
        Long targetUserId = userService.findUserIdByNickname(nickname);
        List<GroupStatus> statuses = List.of(GroupStatus.RECRUITING);
        UserResponseDTO.UserProfileResDTO result = userService.getProfileInfo(targetUserId, statuses);

        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }
}
