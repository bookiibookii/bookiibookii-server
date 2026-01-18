package com.example.bookiibookii.domain.group.controller;

import com.example.bookiibookii.domain.group.dto.req.ApplicationRequestDTO;
import com.example.bookiibookii.domain.group.dto.res.ApplicationResponseDTO;
import com.example.bookiibookii.domain.group.service.ApplicationService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "그룹 관련 API", description = "그룹 및 신청 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/groups")
public class ApplicationController {

    private final ApplicationService applicationService;

    @GetMapping("/{groupId}/applylist")
    @Operation(summary = "신청자 명단 조회 API", description = "방장이 그룹 신청자 목록을 조회합니다. (토큰 대신 userId를 파라미터로 받음)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP4001", description = "해당 그룹을 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "MEMBER4002", description = "방장 권한이 없습니다.")
    })
    @Parameters({
            @Parameter(name = "groupId", description = "조회할 그룹 ID", example = "1")
    })
    public ApiResponse<ApplicationResponseDTO.ApplicationListDTO> getApplicantList(
            @PathVariable(name = "groupId") Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        Long userId = user.getId();
        // 서비스 로직에 전달
        ApplicationResponseDTO.ApplicationListDTO response = applicationService.getApplicantList(groupId, userId);

        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, response);
    }

    @PatchMapping("/apply/{applyId}")
    @Operation(summary = "참여요청 수락/거절 API", description = "신청자의 참여 요청을 수락하거나 거절합니다.")
    @Parameters({
            @Parameter(name = "applyId", description = "신청(Application) ID", example = "10")
    })
    public ApiResponse<ApplicationResponseDTO.UpdateResultDTO> updateApplicationStatus(
            @PathVariable(name = "applyId") Long applyId,
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody ApplicationRequestDTO.UpdateStatusDTO request
    ) {
        Long userId = user.getId();
        ApplicationResponseDTO.UpdateResultDTO result =
                applicationService.updateApplicationStatus(applyId, userId, request.getStatus());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    @PostMapping("/{groupId}/apply")
    @Operation(summary = "그룹 참여 신청 API", description = "게스트가 특정 그룹에 참여 신청을 보냅니다.")
    @Parameters({
            @Parameter(name = "groupId", description = "참여하려는 그룹의 ID", example = "1"),
            @Parameter(name = "userId", description = "현재 로그인한 유저의 ID (테스트용)", example = "2")
    })
    public ApiResponse<ApplicationResponseDTO.JoinResultDTO> joinGroup(
            @PathVariable(name = "groupId") Long groupId,
            @RequestParam(name = "userId") Long userId, // 실제 배포 시에는 Authentication 객체 등으로 교체 예정
            @RequestBody @Valid ApplicationRequestDTO.JoinApplicationDTO request // @Valid로 글자 수 등 검증
    ) {
        ApplicationResponseDTO.JoinResultDTO result = applicationService.joinGroup(groupId, userId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }
}