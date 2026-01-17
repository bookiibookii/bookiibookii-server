package com.example.bookiibookii.domain.tracker.controller.docs;

import com.example.bookiibookii.domain.tracker.dto.response.TrackerDetailResponse;
import com.example.bookiibookii.domain.tracker.dto.response.TrackerHistoryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Tag(name = "Tracker", description = "도서 트래킹 관련 API (상태 조회 및 이력 관리)")
public interface TrackerApi {

    @Operation(
            summary = "트래킹 상세 현황 조회",
            description = "특정 그룹의 현재 트래킹 상태(주자, 반납일, 연장 횟수 등)를 조회합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "트래커를 찾을 수 없음", content = @Content)
    })
    ResponseEntity<TrackerDetailResponse> getTrackerDetail(
            @Parameter(description = "그룹 식별자(ID)", example = "1")
            @PathVariable Long groupId
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
            @Parameter(description = "그룹 식별자(ID)", example = "1")
            @PathVariable Long groupId
    );
}