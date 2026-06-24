package com.example.bookiibookii.domain.policy.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum PolicySuccessCode implements BaseCode {

    POLICY_AGREEMENT_STATUS_OK(HttpStatus.OK, "POLICY200_1", "내 약관 동의 상태 조회 성공"),
    POLICY_AGREE_OK(HttpStatus.OK, "POLICY200_2", "약관 동의 처리 성공"),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
