package com.example.bookiibookii.domain.tracker.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrackerStatus {


//    READING("읽는중"),
//    SHIPPING("배송중"),


    // 1 대 1 매칭만 고려했을 때 enum값
    READY("준비중"),
    HOST_READING("호스트읽는중"),
    SHIPPING_TO_GUEST("게스트에게배송중"),
    RECEIVED("수령완료"),
    GUEST_READING("게스트읽는중"),
    SHIPPING_TO_HOST("회수중"),
    RETURNED("회수완료"),
    COMPLETED("릴레이종료"),
    ;

    private final String description;

}
