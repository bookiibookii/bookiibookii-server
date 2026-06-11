package com.example.bookiibookii.domain.tracker.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TrackerTopBannerType {
    READING_D_DAY(1),
    DIRECT_MEETING_SCHEDULED(2),
    DELIVERY_TRACKING_REGISTER_REQUIRED(3),
    READING_IN_PROGRESS(4),
    DIRECT_MEETING_REGISTER_REQUIRED_HOST(5),
    DIRECT_MEETING_CONFIRM_REQUIRED_GUEST(6),
    EXCHANGE_REVIEW_REQUIRED(7);

    private final int priority;
}
