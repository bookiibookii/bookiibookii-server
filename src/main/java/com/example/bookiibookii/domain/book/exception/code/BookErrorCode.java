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

    // 400 Bad Request
    BLOCKED_CATEGORY(HttpStatus.BAD_REQUEST, "BOOK400_1", "지원하지 않는 카테고리의 도서입니다."),
    INVALID_ISBN(HttpStatus.BAD_REQUEST, "BOOK400_2", "유효한 ISBN13이 필요합니다."),
    INVALID_SEARCH(HttpStatus.BAD_REQUEST, "BOOK400_3", "검색어와 양수인 페이지 및 크기가 필요합니다."),
    PAGES_UNAVAILABLE(HttpStatus.UNPROCESSABLE_CONTENT, "BOOK422_1", "쪽수를 확인할 수 없어 이 책을 등록할 수 없습니다."),
    INCOMPLETE_BOOK(HttpStatus.UNPROCESSABLE_CONTENT, "BOOK422_3", "등록에 필요한 도서 정보가 부족합니다."),
    CATALOG_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "BOOK503_1", "도서 제공처 응답을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
