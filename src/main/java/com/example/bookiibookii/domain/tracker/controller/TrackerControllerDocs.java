package com.example.bookiibookii.domain.tracker.controller;

import com.example.bookiibookii.domain.tracker.dto.req.ReadingProgressRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.*;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Tracker", description = "도서 트래킹 관련 API (상태 조회 및 이력 관리)")
public interface TrackerControllerDocs {

    @GetMapping("/me/trackers")
    @Operation(
            summary = "나의 트래커 전체 리스트 조회",
            description = "나의 모든 트래커(RELAY/TOGETHER)를 조회합니다."
    )
    ApiResponse<List<TrackerListItemResDTO>> getTrackerList(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @GetMapping("/trackers/{groupId}/tracker")
    @Operation(summary = "트래킹 상세 현황 조회")
    ApiResponse<TrackerDetailResDTO> getTrackerDetail(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/trackers/{groupId}/reading-progress")
    @Operation(summary = "독서 진행률 기록")
    ApiResponse<ReadingProgressResponseDTO> updateReadingProgress(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody ReadingProgressRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    /*
    @PostMapping("/{groupId}/tracker/delivery")
    @Operation(summary = "배송 시작 등록", description = "책 읽기를 완료하고 다음 주자에게 배송을 시작할 때 정보를 등록합니다. " +
            "배송 인증 이미지는 Presigned URL로 S3 업로드 후 발급받은 s3Key(형식: image/trackers/{uuid})를 전달하세요. TrackerImage(SENDER_PROOF)로 저장됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배송 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력값(s3Key 형식/중복/S3 미존재)", content = @Content)
    })
    ApiResponse<TrackerDetailResponseDTO> registerShipping(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid TrackerShippingRequestDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );


    // --- 4. 직접 교환(Meeting) 관련 ---
    @GetMapping("/{groupId}/tracker/meetings")
    @Operation(summary = "직접 교환 약속 상세 조회")
    ApiResponse<TrackerMeetingResponseDTO> getMeetingDetail(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/{groupId}/tracker/meetings")
    @Operation(
            summary = "직접 교환 약속 등록/수정",
            description = """
            직접 교환 시 만날 장소와 시간을 등록하거나 수정합니다.
            
            - **최초 등록 시**: 트래커 상태가 `EXCHANGING`(전달 시) 또는 `RETURNING`(반납 시)로 변경됩니다.
            - **수정 시**: 이미 약속이 있는 경우 기존 정보를 업데이트하며, 상대방의 수락 여부(isConfirmed)가 초기화됩니다.
            - **응답**: 수정된 약속의 상세 정보(`TrackerMeetingResponse`)를 반환합니다.
            """
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "약속 등록/수정 성공",
                    content = @Content(schema = @Schema(implementation = TrackerMeetingResponseDTO.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음 (도서 소유자만 가능)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "트래커 또는 약속 정보를 찾을 수 없음")
    })
    ApiResponse<TrackerMeetingResponseDTO> updateMeeting( // 🟢 반환 타입 변경
                                                          @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
                                                          @RequestBody @Valid TrackerMeetingRequestDTO request,
                                                          @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @PatchMapping("/{groupId}/tracker/meetings/completion")
    @Operation(
            summary = "직접 교환 완료 확인 (상호 확인)",
            description = "직접 교환 현장에서 책을 주고받은 후 양측(호스트, 게스트)이 각각 완료 버튼을 누릅니다. " +
                    "두 명 모두 확인 시 소유권이 이전되며, 상태가 `EXCHANGED`(전달 시) 또는 `COMPLETED`(반납 시)로 즉시 변경됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "교환 확인 처리 성공",
                    content = @Content(schema = @Schema(implementation = TrackerDetailResponseDTO.class)))
    })
    ApiResponse<TrackerDetailResponseDTO> completeMeeting(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );
    */

}
