package com.example.bookiibookii.domain.tracker.controller.docs;

import com.example.bookiibookii.domain.tracker.dto.req.TrackerMeetingRequest;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerShippingRequest;
import com.example.bookiibookii.domain.tracker.dto.res.*;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Tracker", description = "도서 트래킹 관련 API (상태 조회 및 이력 관리)")
public interface TrackerApi {

    @Operation(summary = "독서 완료 등록", description = "도서를 다 읽었을 때 호출합니다. 이후 배송 등록이 가능해집니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "독서 완료 등록 성공",
                    content = @Content(schema = @Schema(implementation = TrackerDetailResponse.class)))
    })
    ApiResponse<TrackerDetailResponse> registerReadingDone(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "배송 시작 등록", description = "책 읽기를 완료하고 다음 주자에게 배송을 시작할 때 정보를 등록합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "배송 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력값", content = @Content)
    })
    ApiResponse<TrackerDetailResponse> registerShipping(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid TrackerShippingRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "도서 수령 완료 처리", description = "배송 중인 도서를 수령했을 때 호출합니다.")
    ApiResponse<TrackerDetailResponse> registerReceive(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "독서 시작 등록", description = "도서 수령 후 실제 독서를 시작할 때 호출합니다.")
    ApiResponse<TrackerDetailResponse> registerReading(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "독서 기간 연장 신청", description = "현재 주자의 독서 기간을 연장합니다. (최대 1회 가능)")
    ApiResponse<TrackerDetailResponse> registerExtension(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(description = "연장할 일수", example = "3") @RequestParam(defaultValue = "3") int days,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "나의 트래커 전체 리스트 조회")
    ApiResponse<List<TrackerListResponse>> getTrackerList(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "내가 호스트인 트래커 리스트 조회")
    ApiResponse<List<TrackerListResponse>> getHostTrackers(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "내가 게스트인 트래커 리스트 조회")
    ApiResponse<List<TrackerListResponse>> getGuestTrackers(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "트래킹 상세 현황 조회")
    ApiResponse<TrackerDetailResponse> getTrackerDetail(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "트래킹 히스토리(이력) 조회")
    ApiResponse<List<TrackerHistoryResponse>> getTrackerHistories(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "직접 교환 약속 상세 조회")
    ApiResponse<TrackerMeetingResponse> getMeetingDetail(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "직접 교환 약속 등록/수정", description = "직접 교환 시 만날 장소와 시간을 등록합니다. 등록 시 트래커 상태가 MEETING로 변경됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "약속 등록/수정 성공")
    })
    ApiResponse<TrackerDetailResponse> updateMeeting(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid TrackerMeetingRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "직접 교환 완료 확인(상호 확인)",
            description = "직접 교환 시 책을 주고받은 후 양측이 각각 호출합니다. " +
                    "모두 완료 시 트래커 상태는 RECEIVED(전달 시) 또는 RETURNED(반납 시)로 즉시 변경됩니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "교환 확인 처리 성공")
    })
    @PatchMapping("/{groupId}/tracker/meeting/complete")
    ApiResponse<TrackerDetailResponse> completeMeeting(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );
}