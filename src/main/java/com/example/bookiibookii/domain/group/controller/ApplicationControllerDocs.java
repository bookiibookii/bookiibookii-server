package com.example.bookiibookii.domain.group.controller;


import com.example.bookiibookii.domain.group.dto.req.ApplicationRequestDTO;
import com.example.bookiibookii.domain.group.dto.res.ApplicationResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Application", description = "그룹 및 신청 관리 API")
public interface ApplicationControllerDocs {
    @Operation(summary = "신청자 명단 조회 API", description = "방장이 그룹 신청자 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP404_1", description = "그룹을 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP403_1", description = "Host만 접근 가능한 메뉴입니다.")
    })
    @Parameters({
            @Parameter(name = "groupId", description = "조회할 그룹 ID", example = "1"),
            // userId 파라미터는 토큰에서 자동으로 가져오므로 삭제
    })
    ApiResponse<ApplicationResponseDTO.ApplicationListDTO> getApplicantList(
            @PathVariable(name = "groupId") Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    );
    @Operation(summary = "참여요청 수락/거절 API", description = "신청자의 참여 요청을 수락하거나 거절합니다. (방장 권한 필요)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP404_1", description = "그룹을 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP404_2", description = "신청 내역을 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP403_1", description = "Host만 접근 가능한 메뉴입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_1", description = "이미 처리된 신청 내역입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_2", description = "이미 정원이 가득 찬 그룹입니다.")
    })
    @Parameters({
            @Parameter(name = "applyId", description = "신청(Application) ID", example = "10")
    })
    ApiResponse<ApplicationResponseDTO.UpdateResultDTO> updateApplicationStatus(
            @PathVariable(name = "applyId") Long applyId,
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody ApplicationRequestDTO.UpdateStatusDTO request
    );

    @Operation(summary = "그룹 참여 신청 API", description = "게스트가 특정 그룹에 참여 신청을 보냅니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP404_1", description = "그룹을 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_3", description = "그룹이 RECRUITING 상태가 아닙니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP403_2", description = "Host는 신청할 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_1", description = "이미 신청한 내역이 있습니다.")
    })
    @Parameters({
            @Parameter(name = "groupId", description = "참여하려는 그룹의 ID", example = "1")
    })
    ApiResponse<ApplicationResponseDTO.JoinResultDTO> joinGroup(
            @PathVariable(name = "groupId") Long groupId,
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody @Valid ApplicationRequestDTO.JoinApplicationDTO request
    );

    @Operation(summary = "참여 신청 취소 API", description = "게스트가 본인의 참여 신청을 취소하거나 그룹에서 나갑니다. (모집 중인 그룹만 가능)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공입니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP404_3", description = "해당 그룹의 참여자가 아닙니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP403_3", description = "방장은 취소할 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP400_4", description = "정원이 가득 찼거나 이미 진행 중인 그룹은 취소할 수 없습니다.")
    })
    @Parameters({
            @Parameter(name = "groupId", description = "참여 취소하려는 그룹의 ID", example = "1")
    })
    ApiResponse<ApplicationResponseDTO.CancelResultDTO> cancelApplication(
            @PathVariable(name = "groupId") Long groupId,
            @AuthenticationPrincipal(expression = "user") User user
    );
}
