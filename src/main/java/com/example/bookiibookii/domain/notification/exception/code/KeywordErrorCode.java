package com.example.bookiibookii.domain.notification.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum KeywordErrorCode implements BaseCode {

    KEYWORD_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "KEYWORD400_1", "키워드는 최대 10개까지 등록할 수 있습니다."),

    KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "KEYWORD404_1", "키워드를 찾을 수 없습니다."),
    USER_KEYWORD_NOT_FOUND(HttpStatus.NOT_FOUND, "KEYWORD404_2", "사용자가 등록한 키워드가 아닙니다."),

    DUPLICATE_USER_KEYWORD(HttpStatus.CONFLICT, "KEYWORD409_1", "이미 등록된 키워드입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
