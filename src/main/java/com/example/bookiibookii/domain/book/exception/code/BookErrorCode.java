package com.example.bookiibookii.domain.book.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BookErrorCode implements BaseCode {
    // 404 Not Found
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOK404_1", "책을 찾을 수 없습니다."),
    ALADIN_NOT_FOUND(HttpStatus.NOT_FOUND, "ALADIN404_1", "알라딘 책을 찾을 수 없습니다."),

    // 400 Bad Request
    BLOCKED_CATEGORY(HttpStatus.BAD_REQUEST, "BOOK400_1", "지원하지 않는 카테고리의 도서입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
