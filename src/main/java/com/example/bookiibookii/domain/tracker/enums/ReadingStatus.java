package com.example.bookiibookii.domain.tracker.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReadingStatus {

    IDLE("초기"),
    READING("자신의 책 읽는 중"),
    READ_DONE("자신의 책 읽기 완료"),
    REVIEW_DONE("자신의 책 후기 완료, 교환 준비"),
    READING_2("파트너 책 읽는 중"),
    READ_DONE_2("파트너 책 읽기 완료"),
    REVIEW_DONE_2("파트너 책 후기 완료, 반납 준비"),
    DONE("반납 완료");

    private final String description;
}
