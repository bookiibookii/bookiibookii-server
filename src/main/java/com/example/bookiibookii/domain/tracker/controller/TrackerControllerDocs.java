package com.example.bookiibookii.domain.tracker.controller;

import com.example.bookiibookii.domain.tracker.dto.req.ExtendReadingPeriodReqDTO;
import com.example.bookiibookii.domain.tracker.dto.req.ReadingProgressRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.*;
import com.example.bookiibookii.domain.tracker.dto.res.ExtendReadingPeriodResDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Tracker", description = "도서 트래킹 관련 API")
public interface TrackerControllerDocs {

    @GetMapping("/me/trackers")
    @Operation(
            summary = "나의 트래커 전체 리스트 조회",
            description = """
            나의 모든 트래커(RELAY/TOGETHER)를 조회합니다.
            - summary는 같은 조회 조건으로 내려가는 items의 displayStatus 기준으로 계산됩니다.
            - readingCount: READING
            - exchangingCount: TRACKING_REQUIRED, SHIPPING, RETURN_TRACKING_REQUIRED, RETURNING, MEETING_REQUIRED, EXCHANGING
            - reviewCount: REVIEW_WRITING, EXCHANGE_REVIEW_WRITING
            """
    )
    ApiResponse<TrackerListResDTO> getTrackerList(
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


    @PatchMapping("/trackers/{groupId}/reading-period")
    @Operation(
            summary = "독서기간 수정",
            description = """
            호스트만 독서기간을 수정할 수 있습니다.
            - MY_BOOK_READING 또는 PARTNER_BOOK_READING 단계에서만 가능합니다.
            - newEndDate는 오늘보다 이후여야 합니다.
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRACKER200_23", description = "독서기간 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRACKER403_2", description = "해당 그룹의 멤버가 아닙니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRACKER403_6", description = "HOST만 독서기간을 수정할 수 있습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRACKER404_1", description = "트래커를 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRACKER400_26", description = "독서단계가 아닙니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "TRACKER400_25", description = "오늘 이후의 날짜를 선택해야 합니다.")
    })
    ApiResponse<ExtendReadingPeriodResDTO> extendReadingPeriod(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody ExtendReadingPeriodReqDTO request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );
}
