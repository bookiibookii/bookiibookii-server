package com.example.bookiibookii.domain.support.report.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReportErrorCode implements BaseCode {
    REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "Report404_1", "해당 신고 내역을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}