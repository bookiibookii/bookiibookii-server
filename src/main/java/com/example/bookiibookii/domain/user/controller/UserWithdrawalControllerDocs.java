package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.user.dto.req.UserRequestDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "User", description = "유저 관련 API")
public interface UserWithdrawalControllerDocs {

    @Operation(summary = "회원 탈퇴 API", description = "탈퇴 사유를 저장하고 회원 탈퇴를 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "회원 탈퇴 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (탈퇴 사유 미입력 등)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    ApiResponse<Void> withdraw(
            @AuthenticationPrincipal(expression = "user") User user,
            @Valid @RequestBody UserRequestDTO.WithdrawalReqDTO request
    );
}
