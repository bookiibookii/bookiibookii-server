package com.example.bookiibookii.domain.tracker.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TrackerErrorCode implements BaseCode {
    TRACKER_NOT_FOUND(HttpStatus.NOT_FOUND, "T001", "해당 트래커를 찾을 수 없습니다."),
    INVALID_TRACKER_STATUS(HttpStatus.BAD_REQUEST, "T002", "상태 변경이 불가능한 단계입니다."),
    NOT_YOUR_TURN(HttpStatus.FORBIDDEN, "T003", "현재 독서 주자가 아닙니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
