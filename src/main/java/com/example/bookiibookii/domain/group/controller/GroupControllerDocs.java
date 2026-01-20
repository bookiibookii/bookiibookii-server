package com.example.bookiibookii.domain.group.controller;


import com.example.bookiibookii.domain.group.dto.req.GroupRequestDTO;
import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Group API", description = "그룹 생성 및 관리 관련 API")
public interface GroupControllerDocs {
    @Operation(summary = "그룹 생성 API", description = "새로운 독서 그룹(이어읽기/함께읽기)을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_4", description = "도서 미선택"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_5", description = "부적절한 시작 날짜"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_7", description = "호스트 장소 정보 없음")
    })
    ApiResponse<GroupResponseDTO.CreateResultDTO> createGroup(
            @AuthenticationPrincipal User host,
            @RequestBody @Valid GroupRequestDTO.CreateDTO request
    );
}
