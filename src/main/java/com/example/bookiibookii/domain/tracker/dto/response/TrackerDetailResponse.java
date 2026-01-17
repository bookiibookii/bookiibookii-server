package com.example.bookiibookii.domain.tracker.dto.response;

import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
public class TrackerDetailResponse {
    private Long trackerId;
    private TrackerStatus trackerStatus;   //도서상태
    private Long currentMatchedMemberId; // 현재 주자 식별자
    private LocalDateTime endDate;       // 반납 예정일
    private Integer extension_count;    // 연장횟수
    private Integer extension_days;     // 연장일수
}
