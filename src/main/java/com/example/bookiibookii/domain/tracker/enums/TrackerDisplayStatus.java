package com.example.bookiibookii.domain.tracker.enums;

public enum TrackerDisplayStatus {
    // 프론트에서 노출되는 값들, db 저장 x

    READING,              // 읽는 중
    REVIEW_WRITING,       // 후기 작성
    REVIEW_WAITING_PARTNER, // 내 후기 작성 완료, 파트너 후기 대기

    TRACKING_REQUIRED,    // 운송장 등록 필요
    SHIPPING,             // 배송 중
    RETURN_TRACKING_REQUIRED, // 반납 운송장 등록 필요
    RETURNING,            // 반납 중
    WAITING_PARTNER_TRACKING_REGISTER, // 상대 운송장 등록 대기
    WAITING_PARTNER_RECEIPT_CONFIRM, // 상대 수령 인증 대기

    MEETING_REQUIRED,     // 약속 등록 필요
    MEETING_REGISTER_REQUIRED, // 호스트 약속 등록 필요
    WAITING_HOST_MEETING_REGISTER, // 게스트가 호스트 약속 등록 대기
    WAITING_PARTNER_MEETING_COMPLETE, // 상대 교환 완료 처리 대기
    EXCHANGING,           // 교환 중

    EXCHANGE_REVIEW_WRITING, // 교환 후기 작성
    EXCHANGE_REVIEW_WAITING_PARTNER, // 내 교환 후기 작성 완료, 파트너 후기 대기
    COMPLETED // 교환독서 종료
}
