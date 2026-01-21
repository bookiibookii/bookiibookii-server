package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

public interface UserControllerDocs {
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
            유저의 닉네임과 초기 태그를 저장하는 API입니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "온보딩 저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "온보딩 저장 실패")
    })
    ApiResponse<Void> createUserOnboarding(User user, @Valid @RequestBody UserRequestDTO.OnboardingReqDTO request);
}
