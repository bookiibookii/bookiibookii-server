package com.example.bookiibookii.domain.tracker.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReadingStatus {

    IDLE("초기"),
    MY_BOOK_READING("자신의 책 읽는 중"),
    MY_BOOK_READ_DONE("자신의 책 읽기 완료"),
    MY_BOOK_REVIEW_DONE("자신의 책 후기 완료, 교환 준비"),
    PARTNER_BOOK_READING("파트너 책 읽는 중"),
    PARTNER_BOOK_READ_DONE("파트너 책 읽기 완료"),
    PARTNER_BOOK_REVIEW_DONE("파트너 책 후기 완료, 반납 준비"),
    DONE("반납 완료");

    private final String description;
}
