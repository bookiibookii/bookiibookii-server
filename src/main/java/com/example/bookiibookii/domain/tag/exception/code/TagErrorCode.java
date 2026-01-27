package com.example.bookiibookii.domain.tag.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TagErrorCode implements BaseCode {
    INVALID_TAG_CODE(HttpStatus.BAD_REQUEST,
            "TAG400_1",
            "존재하지 않는 태그가 입력되었습니다.")
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}