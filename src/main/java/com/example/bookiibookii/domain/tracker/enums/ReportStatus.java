package com.example.bookiibookii.domain.tracker.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportStatus {
    PENDING("대기 중"),
    IN_PROGRESS("처리 중"),
    RESOLVED("해결 완료");

    private final String description;
}