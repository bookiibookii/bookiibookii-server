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
            "해당 사용자를 찾을 수 없습니다."),
    USER_TAG_NOT_FOUND(HttpStatus.NOT_FOUND,
            "USERTAG404_1",
            "사용자의 태그가 존재하지 않습니다."),
    USER_WITHDRAWN(HttpStatus.FORBIDDEN,
            "USER403_1",
            "탈퇴한 사용자입니다.")
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
