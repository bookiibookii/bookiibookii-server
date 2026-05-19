package com.example.bookiibookii.domain.tracker.controller;

import com.example.bookiibookii.domain.tracker.dto.req.MeetingRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.MeetingResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "Meeting", description = "직접 교환 약속 등록/수정/조회/완료 API")
@RequestMapping("/api/groups/{groupId}/meetings")
public interface MeetingControllerDocs {

    @PostMapping
    @Operation(
            summary = "직접 교환 약속 등록",
            description = """
            HOST가 직접교환 약속을 등록합니다.
            
            - 직접교환 그룹에서만 가능합니다.
            - 두 matchedMember 모두 EXCHANGING 또는 모두 RETURNING 상태여야 합니다.
            - 등록 성공 시 두 matchedMember의 ExchangeStatus가 MEETING_SCHEDULED로 변경됩니다.
            - scheduledAt이 미래여도 이후 교환완료 확인이 가능합니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "약속 등록 성공",
                    content = @Content(schema = @Schema(implementation = MeetingResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "직접교환 그룹이 아니거나 교환 단계가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "HOST가 아니거나 그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "그룹 또는 장소를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 약속이 존재함")
    })
    ApiResponse<MeetingResponseDTO> createMeeting(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid MeetingRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping
    @Operation(
            summary = "직접 교환 약속 수정",
            description = """
            HOST가 groupId 기준으로 현재 등록된 직접교환 약속을 수정합니다.
            
            - 프론트가 meetingId를 들고 있지 않아도 됩니다.
            - 수정 가능 필드: locationId, addressDetail, scheduledAt
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 수정 성공",
                    content = @Content(schema = @Schema(implementation = MeetingResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "직접교환 그룹이 아니거나 교환 단계가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "HOST가 아니거나 그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속 또는 장소를 찾을 수 없음")
    })
    ApiResponse<MeetingResponseDTO> updateMeeting(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid MeetingRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @GetMapping
    @Operation(
            summary = "직접 교환 약속 상세 조회",
            description = """
            HOST/GUEST가 groupId 기준으로 등록된 약속 정보를 조회합니다.
            
            - 조회 전 현재 로그인 사용자가 교환 단계(EXCHANGING 또는 RETURNING)인지 먼저 검증합니다.
            - 교환완료 버튼 활성화 여부는 별도로 계산하지 않습니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 조회 성공",
                    content = @Content(schema = @Schema(implementation = MeetingResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "직접교환 그룹이 아니거나 교환 단계가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속을 찾을 수 없음")
    })
    ApiResponse<MeetingResponseDTO> getMeeting(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/completion")
    @Operation(
            summary = "직접 교환 완료 확인",
            description = """
            HOST/GUEST가 각각 직접 교환 완료를 확인합니다.
            
            - 약속 시간이 아직 지나지 않아도 완료 처리 가능합니다.
            - 한 명만 완료하면 상대방 확인을 기다립니다.
            - 두 명 모두 완료하면 1차 교환 단계에서는 PARTNER_BOOK_READING으로 전환됩니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "교환 완료 확인 성공",
                    content = @Content(schema = @Schema(implementation = MeetingResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "직접교환 그룹이 아니거나 교환 완료 가능 단계가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "그룹 멤버가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "약속을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 교환 완료 처리됨")
    })
    ApiResponse<MeetingResponseDTO> completeMeeting(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );
}
