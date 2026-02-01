package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.domain.user.dto.res.PresignedUrlResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Tag(name = "User", description = "유저 관련 API")
public interface UserControllerDocs {

    @Operation(
            summary = "사용자 이미지 업로드용 Presigned URL 발급",
            description = """
            온보딩 또는 프로필 이미지 업로드를 위한 Presigned URL을 발급합니다.
            - s3Key 형식: image/users/{userId}/{uuid}
            - 발급된 presignedPutUrl로 PUT 요청 후, 온보딩 API 등에서 s3Key를 전달해 저장합니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Presigned URL 발급 성공")
    })
    @PostMapping("/api/users/me/image/presigned-url")
    ApiResponse<PresignedUrlResponseDTO> getPresignedPutUrlForUserImage(
            @AuthenticationPrincipal(expression = "user") User user
    );

    // api/users/name-validation
    @Operation(
            summary = "닉네임 중복 검증 API",
            description = """
            닉네임의 중복 여부를 검증합니다.

            - TRUE : 사용 가능한 닉네임입니다.
            - FALSE : 이미 존재하는 닉네임입니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "중복 여부 검증 성공")
    })
    ApiResponse<Map<String, Boolean>> validateNickname(@NotNull @RequestParam String nickname);


    // api/users/onboarding
    @Operation(
            summary = "온보딩 기능 API",
            description = """
            유저의 닉네임, 초기 태그, 프로필 이미지를 저장합니다.
            - 이미지는 선택: /api/users/me/image/presigned-url 로 Presigned URL 발급 후 업로드하고, 받은 s3Key를 본 API의 s3Key에 넣어 호출합니다.
            - s3Key를 넣지 않으면 프로필 이미지는 null로 둡니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "온보딩 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "온보딩 저장 실패")
    })
    ApiResponse<Void> createUserOnboarding(@AuthenticationPrincipal User user, @Valid @RequestBody UserRequestDTO.OnboardingReqDTO request);

    // api/mypage
    @Operation(
            summary = "마이페이지 조회 API",
            description = """
            마이페이지 조회하는 API입니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "마이페이지 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "마이페이지 조회 실패")
    })
    ApiResponse<UserResponseDTO.UserProfileResDTO> getMypage(@AuthenticationPrincipal User user);

    // api/profiles/{nickname}
    @Operation(
            summary = "타 유저 프로필 조회 API",
            description = """
            타 유저의 프로필을 조회하는 API입니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "프로필 조회 실패")
    })
    ApiResponse<UserResponseDTO.UserProfileResDTO> getOtherProfile(@PathVariable("nickname") String nickname);
}
