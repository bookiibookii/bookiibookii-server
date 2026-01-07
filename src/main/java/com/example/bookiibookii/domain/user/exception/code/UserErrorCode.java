package com.example.bookiibookii.domain.user.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseCode {
    NOT_FOUND(HttpStatus.NOT_FOUND,
            "USER404_1",
            "해당 사용자를 찾을 수 없습니다.")
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
