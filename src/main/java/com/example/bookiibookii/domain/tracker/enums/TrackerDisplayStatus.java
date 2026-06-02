package com.example.bookiibookii.domain.tracker.enums;

public enum TrackerDisplayStatus {
    // 프론트에서 노출되는 값들

    READING,              // 읽는 중
    REVIEW_WRITING,       // 후기 작성
    REVIEW_WAITING_PARTNER, // 내 후기 작성 완료, 파트너 후기 대기

    TRACKING_REQUIRED,    // 운송장 등록 필요
    SHIPPING,             // 배송 중
    RETURN_TRACKING_REQUIRED, // 반납 운송장 등록 필요
    RETURNING,            // 반납 중

    MEETING_REQUIRED,     // 약속 등록 필요
    EXCHANGING,           // 교환 중

    EXCHANGE_REVIEW_WRITING // 교환 후기 작성
}
