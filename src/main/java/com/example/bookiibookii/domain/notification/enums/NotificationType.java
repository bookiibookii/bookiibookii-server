package com.example.bookiibookii.domain.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {

    // GROUP / MATCHING (SYSTEM)
    GROUP_JOIN_REQUEST(NotificationCategory.SYSTEM),          // GRP-030 (요청관리)
    GROUP_REQUEST_ACCEPTED(NotificationCategory.SYSTEM),
    GROUP_REQUEST_REJECTED(NotificationCategory.SYSTEM),
    GROUP_MATCH_SUCCESS(NotificationCategory.SYSTEM),         // TRK-010 (트래커)
    GROUP_MATCH_REJECTED(NotificationCategory.SYSTEM),        // GRP-001 (리스트)
    GROUP_MATCH_AUTO_REJECTED(NotificationCategory.SYSTEM),   // GRP-001 (리스트)
    GROUP_COMMENT_CREATED(NotificationCategory.SYSTEM),       // GRP-010 (그룹 상세/댓글)
    GROUP_COMMENT_REPLIED(NotificationCategory.SYSTEM),
    GROUP_DELETED(NotificationCategory.SYSTEM),               // 문의하기 or GRP-001
    GROUP_MATCH_FAILED_BY_EXPIRE(NotificationCategory.SYSTEM),// GRP-001 (리스트)
    GROUP_MATCH_FAILED_BY_CAPACITY(NotificationCategory.SYSTEM), // GRP-001 (리스트)

    // TRACKER (SYSTEM) - 택배/독서 트래킹
    TRACKER_READING_STARTED(NotificationCategory.SYSTEM),     // TRK-010
    TRACKER_PERIOD_EXTENDED(NotificationCategory.SYSTEM),     // TRK-010
    TRACKER_READING_REVIEW_COMPLETED(NotificationCategory.SYSTEM),
    TRACKER_SHIPMENT_REGISTERED(NotificationCategory.SYSTEM), // TRK-010
    TRACKER_DELIVERY_CONFIRMED(NotificationCategory.SYSTEM),  // TRK-010
    TRACKER_DELIVERY_RECEIVE_REMINDER(NotificationCategory.SYSTEM), // TRK-010
    TRACKER_RETURN_SHIPMENT_REGISTERED(NotificationCategory.SYSTEM), // TRK-010 (반납 출발)
    // TODO: 외부 택배사 상태 API 연동 전까지 배송 상태 업데이트 알림(NOTI-DEL-002)은 구현하지 않는다.
    TRACKER_EXCHANGE_COMPLETED(NotificationCategory.SYSTEM),  // TRK-030 (후기작성)
    TRACKER_COMMENT_CREATED(NotificationCategory.SYSTEM),
    TRACKER_EXCHANGE_REVIEW_CREATED(NotificationCategory.SYSTEM),

    // DIRECT EXCHANGE
    DIRECT_MEETING_CREATED(NotificationCategory.SYSTEM),
    DIRECT_MEETING_UPDATED(NotificationCategory.SYSTEM),
    DIRECT_MEETING_CONFIRM_REMINDER(NotificationCategory.SYSTEM),

    // LIBRARY
    READING_CARD_REACTION_CREATED(NotificationCategory.SYSTEM),

    // KEYWORD
    KEYWORD_GROUP_CREATED(NotificationCategory.KEYWORD),      // GRP-010
    ;

    private final NotificationCategory category;
}
