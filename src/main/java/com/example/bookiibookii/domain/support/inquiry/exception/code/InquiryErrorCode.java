package com.example.bookiibookii.domain.support.inquiry.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum InquiryErrorCode implements BaseCode {

    // --- 400 BAD_REQUEST ---
    INQUIRY_ALREADY_ANSWERED(HttpStatus.BAD_REQUEST, "INQUIRY400_1", "이미 답변이 완료된 문의입니다."),
    INVALID_INQUIRY_STATUS(HttpStatus.BAD_REQUEST, "INQUIRY400_2", "답변 가능한 상태가 아닙니다."),

    // --- 403 FORBIDDEN ---
    NOT_INQUIRY_WRITER(HttpStatus.FORBIDDEN, "INQUIRY403_1", "해당 문의의 작성자가 아닙니다. 조회 권한이 없습니다."),

    // --- 404 NOT_FOUND ---
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "INQUIRY404_1", "해당 문의를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}