package com.example.bookiibookii.domain.support.faq.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FaqErrorCode implements BaseCode {
    FAQ_NOT_FOUND(HttpStatus.NOT_FOUND, "FAQ404_1", "FAQ를 찾을 수 없습니다."),
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
