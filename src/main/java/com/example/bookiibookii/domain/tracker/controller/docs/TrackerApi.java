package com.example.bookiibookii.domain.tracker.controller.docs;

import com.example.bookiibookii.domain.tracker.dto.req.TrackerMeetingRequest;
import com.example.bookiibookii.domain.tracker.dto.req.TrackerShippingRequest;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerHistoryResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerMeetingResponse;
import com.example.bookiibookii.domain.user.entity.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Tag(name = "Tracker", description = "도서 트래킹 관련 API (상태 조회 및 이력 관리)")
public interface TrackerApi {

    @Operation(summary = "독서 완료 등록", description = "도서를 다 읽었을 때 호출합니다. 이후 배송 등록이 가능해집니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "독서 완료 등록 성공",
                    content = @Content(schema = @Schema(implementation = TrackerDetailResponse.class)))
    })
    ResponseEntity<TrackerDetailResponse> registerReadingDone(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "배송 시작 등록",
            description = "책 읽기를 완료하고 다음 주자에게 배송을 시작할 때 정보를 등록합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "배송 등록 성공",
                    content = @Content(schema = @Schema(implementation = TrackerDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 입력값 (Validation 실패)", content = @Content),
            @ApiResponse(responseCode = "404", description = "트래커 또는 다음 멤버를 찾을 수 없음", content = @Content)
    })
    ResponseEntity<TrackerDetailResponse> registerShipping(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid TrackerShippingRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "도서 수령 완료 처리",
            description = "배송 중인 도서를 수령했을 때 호출합니다. 상태가 RECEIVED 또는 RETURNED로 변경됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수령 확인 성공",
                    content = @Content(schema = @Schema(implementation = TrackerDetailResponse.class))),
            @ApiResponse(responseCode = "404", description = "트래커를 찾을 수 없음", content = @Content)
    })
    ResponseEntity<TrackerDetailResponse> registerReceive(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "독서 시작 등록",
            description = "도서 수령 후 실제 독서를 시작할 때 호출합니다. 독서 종료 예정일(endDate)이 이때 계산됩니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "독서 시작 등록 성공",
                    content = @Content(schema = @Schema(implementation = TrackerDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "이미 독서 중이거나 잘못된 상태", content = @Content)
    })
    ResponseEntity<TrackerDetailResponse> registerReading(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "독서 기간 연장 신청",
            description = "현재 주자의 독서 기간을 연장합니다. (최대 1회 가능)"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "기간 연장 성공",
                    content = @Content(schema = @Schema(implementation = TrackerDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "연장 횟수 초과", content = @Content)
    })
    ResponseEntity<TrackerDetailResponse> registerExtension(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(description = "연장할 일수", example = "3") @RequestParam(defaultValue = "3") int days,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "나의 트래커 전체 리스트 조회",
            description = "로그인한 사용자가 참여 중인 모든 그룹의 트래킹 현황과 4단계 타임라인 날짜를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    ResponseEntity<List<TrackerListResponse>> getTrackerList(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "내가 호스트인 트래커 리스트 조회",
            description = "내가 책 주인(호스트)으로 참여 중인 트래커 리스트만 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    ResponseEntity<List<TrackerListResponse>> getHostTrackers(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(summary = "내가 게스트인 트래커 리스트 조회",
            description = "내가 빌려 읽는 사람(게스트)으로 참여 중인 트래커 리스트만 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    ResponseEntity<List<TrackerListResponse>> getGuestTrackers(
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "트래킹 상세 현황 조회",
            description = "특정 그룹의 현재 트래킹 상태(주자, 반납일, 연장 횟수 등)를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "트래커를 찾을 수 없음", content = @Content)
    })
    ResponseEntity<TrackerDetailResponse> getTrackerDetail(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );

    @Operation(
            summary = "트래킹 히스토리(이력) 조회",
            description = "해당 그룹의 도서가 거쳐온 모든 배송 및 수령 기록을 최신순으로 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "이력 조회 성공"),
            @ApiResponse(responseCode = "404", description = "이력을 찾을 수 없음", content = @Content)
    })
    ResponseEntity<List<TrackerHistoryResponse>> getTrackerHistories(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );
    @Operation(
            summary = "직접 교환 약속 상세 조회",
            description = "특정 그룹의 직접 교환 약속 장소와 시간을 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "약속 정보가 존재하지 않음", content = @Content)
    })
    ResponseEntity<TrackerMeetingResponse> getMeetingDetail(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );
    @Operation(
            summary = "직접 교환 약속 등록/수정",
            description = "직접 교환 방식일 때 만날 장소와 시간을 등록하거나 수정합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "직접 교환 등록/수정 성공"),
            @ApiResponse(responseCode = "404", description = "직접교환 실패", content = @Content)
    })
    ResponseEntity<TrackerDetailResponse> updateMeeting(
            @PathVariable Long groupId,
            @RequestBody @Valid TrackerMeetingRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal(expression = "user") User user
    );
}