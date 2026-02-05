package com.example.bookiibookii.domain.userbook.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CardErrorCode implements BaseCode {

    PAGE_EXCEEDS_TOTAL(HttpStatus.BAD_REQUEST, "CARD400_1", "입력하신 페이지가 도서의 전체 페이지를 초과합니다."),
    INVALID_PAGE_VALUE(HttpStatus.BAD_REQUEST, "CARD400_2", "페이지 번호는 0보다 커야 합니다."),

    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "CARD404_1", "해당 독서 카드를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

}
