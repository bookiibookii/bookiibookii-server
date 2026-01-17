package com.example.bookiibookii.domain.tracker.controller.docs;

import com.example.bookiibookii.domain.tracker.dto.response.TrackerDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "Tracker", description = "도서 트래킹 관련 API")
public interface TrackerApi {

    @Operation(
            summary = "트래킹 상세 현황 조회",
            description = "특정 트래커의 현재 주자, 반납 예정일, 연장 횟수 등을 조회합니다."
    )
    ResponseEntity<TrackerDetailResponse> getTrackerDetail(
            @Parameter(description = "트래커 식별자(ID)", example = "1")
            @PathVariable Long trackerId
    );
}