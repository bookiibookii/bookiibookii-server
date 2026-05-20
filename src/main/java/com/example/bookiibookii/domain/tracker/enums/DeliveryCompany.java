package com.example.bookiibookii.domain.tracker.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DeliveryCompany {
    CJ_LOGISTICS("CJ대한통운"),
    HANJIN("한진택배"),
    LOTTE("롯데택배"),
    POST_OFFICE("우체국택배"),
    LOGEN("로젠택배"),
    CU("CU 편의점택배"),
    GS("GS 편의점택배");

    private final String displayName;
}
