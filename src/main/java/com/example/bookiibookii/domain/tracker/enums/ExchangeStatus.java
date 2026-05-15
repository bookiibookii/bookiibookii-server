package com.example.bookiibookii.domain.tracker.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExchangeStatus {
    NOT_STARTED("아직 교환 단계 아님"),

    // 택배 교환
    TRACKING_REGISTER_WAITING("운송장 등록 대기"),
    TRACKING_REGISTERED("내가 보낸 운송장 등록 완료"),
    RECEIVED_CONFIRMED("내가 상대방 책 수령 인증 완료"),

    // 직접 교환
    MEETING_SCHEDULE_WAITING("약속 등록/확인 대기"),
    MEETING_SCHEDULED("약속 등록 완료, 약속일 대기"),
    MEETING_COMPLETED("내가 교환 완료 처리"),
    MEETING_FAILED("내가 교환 미완료 처리");

    private final String description;
}
