package com.example.bookiibookii.domain.tracker.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrackerStatus {

    READY("매칭완료, 독서 전"),
    READING("자신의 책 읽는 중"),
    READ_DONE("1차 독서 완료, 교환 준비"),
    EXCHANGING("1차 교환 진행 중"),
    EXCHANGED("1차 교환 완료"),
    READING_2("파트너 책 읽는 중"),
    READ_DONE_2("2차 독서 완료, 반납 준비"),
    RETURNING("2차 교환(반납) 진행 중"),
    COMPLETED("릴레이 종료");

    private final String description;
}
