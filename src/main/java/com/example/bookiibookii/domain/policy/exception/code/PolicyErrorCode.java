package com.example.bookiibookii.domain.policy.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PolicyErrorCode implements BaseCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "POLICY404_1", "존재하지 않는 사용자입니다."),
    INVALID_POLICY_INCLUDED(HttpStatus.BAD_REQUEST, "POLICY400_1", "현재 유효하지 않은 약관이 포함되어 있습니다."),
    REQUIRED_POLICY_NOT_AGREED(HttpStatus.BAD_REQUEST, "POLICY400_2", "필수 약관에 동의해야 합니다."),
    POLICY_AGREEMENT_EMPTY(HttpStatus.BAD_REQUEST, "POLICY400_3", "약관 동의 요청이 비어 있습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}