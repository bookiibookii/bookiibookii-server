package com.example.bookiibookii.domain.notification.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum KeywordSuccessCode implements BaseCode{
    KEYWORD_LIST_OK(HttpStatus.OK, "KEYWORD200_1", "키워드 목록 조회 성공"),
    KEYWORD_CREATE_OK(HttpStatus.OK, "KEYWORD200_2", "키워드 등록 성공"),
    KEYWORD_DELETE_OK(HttpStatus.OK, "KEYWORD200_3", "키워드 삭제 성공");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
