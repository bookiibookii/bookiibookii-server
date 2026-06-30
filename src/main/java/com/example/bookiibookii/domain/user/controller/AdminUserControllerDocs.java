package com.example.bookiibookii.domain.user.controller;

import com.example.bookiibookii.domain.user.dto.res.UserResponseDTO;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "Admin", description = "관리자용 API")
public interface AdminUserControllerDocs {

    @Operation(
            summary = "관리자 계정 목록 조회 API",
            description = """
            ADMIN 권한을 가진 계정 목록을 조회합니다.
            - id: 계정 고유 ID
            - nickname: 닉네임
            - introduction: 이메일 정보
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "관리자 계정 목록 조회 성공")
    })
    ApiResponse<List<UserResponseDTO.AdminUserListDTO>> getAdminUsers();
}
