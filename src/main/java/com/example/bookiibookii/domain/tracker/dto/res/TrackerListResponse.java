package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TrackerListResponse {
    private Long groupId;
    private String bookTitle;
    private String author;
    private String targetNickname;

    // 핵심: 앞에서부터 순서대로 들어있는 날짜 리스트
    // 예: ["12. 16.", "12. 20."] -> 1, 2단계만 날짜 표시 및 활성화
    private List<String> stepDates;
}