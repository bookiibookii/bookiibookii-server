package com.example.bookiibookii.domain.tracker.controller.docs;

import com.example.bookiibookii.domain.tracker.dto.req.TrackerShippingRequest;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerHistoryResponse;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Tracker", description = "도서 트래킹 관련 API (상태 조회 및 이력 관리)")
public interface TrackerApi {

    @Operation(
            summary = "배송 시작 등록",
            description = "책 읽기를 완료하고 다음 주자에게 배송을 시작할 때 정보를 등록합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "배송 등록 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 입력값 (Validation 실패)", content = @Content),
            @ApiResponse(responseCode = "404", description = "트래커 또는 다음 멤버를 찾을 수 없음", content = @Content)
    })
    ResponseEntity<Void> registerShipping(
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId,
            @RequestBody @Valid TrackerShippingRequest request
    );

    @Operation(
            summary = "내 트래커 리스트 조회",
            description = "로그인한 사용자가 참여 중인 모든 그룹의 트래킹 현황과 4단계 타임라인 날짜를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "403", description = "권한 없음", content = @Content)
    })
    ResponseEntity<List<TrackerListResponse>> getTrackerList(
//            @Parameter(hidden = true) Long userId
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
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId
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
            @Parameter(description = "그룹 식별자(ID)", example = "1") @PathVariable Long groupId
    );
}
