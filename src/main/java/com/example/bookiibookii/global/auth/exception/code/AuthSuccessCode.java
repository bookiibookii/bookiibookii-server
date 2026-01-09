package com.example.bookiibookii.global.auth.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthSuccessCode implements BaseCode {
    LOGOUT_SUCCESS(HttpStatus.OK,
            "AUTH200_1",
            "로그아웃을 성공했습니다.")
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
