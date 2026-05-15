package com.example.bookiibookii.domain.memberbook.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberBookErrorCode implements BaseCode {

    MEMBER_BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "MB404_1", "해당 멤버북을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
