package com.example.bookiibookii.domain.tracker.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrackerStatus {

    READY("준비중"),
    READING("읽는중"),
    SHIPPING("배송중"),
    RECEIVED("수령완료"),
    RETURNED("회수완료"),
    COMPLETED("릴레이종료");

    private final String description;

}
