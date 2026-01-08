package com.example.bookiibookii.domain.user.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserSuccessCode implements BaseCode {
    OK(HttpStatus.OK,
            "USER200_1",
            "사용자를 찾았습니다.")
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
