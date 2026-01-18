package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrackerListResponse {
    private Long groupId;
    private String bookTitle;      // 도서 제목
    private TrackerStatus status;  // 트래커 상태
    private String role;           // 내 역할 (HOST / GUEST)
}