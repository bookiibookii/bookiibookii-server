package com.example.bookiibookii.domain.support.notice.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NoticeErrorCode implements BaseCode {
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND,
            "NOTICE404_1",
            "공지글을 찾을 수 없습니다."),
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
