package com.example.bookiibookii.domain.tracker.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReadingStatus {

    MY_BOOK_READING("자신의 책 읽는 중"),
    MY_BOOK_REVIEWING("1차 독서 완료, 후기 작성 중"),
    EXCHANGING("1차 교환 진행 중"),
    EXCHANGED("1차 교환 완료"),
    PARTNER_BOOK_READING("파트너 책 읽는 중"),
    PARTNER_BOOK_REVIEWING("2차 독서 완료, 후기 작성 중"),
    RETURNING("2차 교환(반납) 진행 중"),
    COMPLETED("릴레이 종료");

    private final String description;
}
