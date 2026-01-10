package com.example.bookiibookii.domain.tracker.enums;


import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrackerStatus {

    HOST_READING("호스트 읽는 중"),
    SHIPPING_TO_GUEST("게스트에게 배송 중"),
    GUEST_READING("게스트 읽는 중"),
    SHIPPING_TO_HOST("호스트에게 배송 중"),
    RETURNED("회수완료");

    private final String description;

}
