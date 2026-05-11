package com.example.bookiibookii.domain.tracker.enums;

import com.example.bookiibookii.domain.notification.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.example.bookiibookii.domain.notification.enums.NotificationType.*;

@Getter
@AllArgsConstructor
public enum TrackerNotiType {

    READING_STARTED(
            TRACKER_READING_STARTED,
            "파트너가 책을 읽기 시작했어요!",
            "{nickname}님이 {bookTitle} 읽기를 시작했습니다."
    ),

    EXTEND_REQUESTED(
            TRACKER_PERIOD_EXTENDED,
            "독서 기간이 조금 늘어났어요",
            "{nickname}님이 책을 더 깊이 읽기 위해 독서 기간을 연장했어요. (반납 예정일: {returnDueAt})"
    ),

    READING_FINISHED(
            TRACKER_READING_FINISHED,
            "파트너가 책을 다 읽었어요",
            "{nickname}님이 책을 다 읽었어요. 곧 소중한 책을 만날 수 있어요!"
    ),

    SHIPPING_REGISTERED(
            TRACKER_SHIPMENT_REGISTERED,
            "소중한 책이 오고 있어요!",
            "{nickname}님이 책을 보내셨어요. 운송장 번호를 확인하고 배송을 조회해보세요."
    ),

    RECEIVED_CONFIRMED(
            TRACKER_DELIVERY_CONFIRMED,
            "책이 무사히 도착했대요!",
            "{nickname}님이 책을 잘 받으셨다고 해요. 수령 인증을 확인해볼까요?"
    ),

    RETURN_SHIPPING_REGISTERED(
            TRACKER_RETURN_SHIPMENT_REGISTERED,
            "책이 집으로 돌아오고 있어요",
            "{nickname}님이 독서를 마치고 책을 반납했어요. 곧 소중한 책을 다시 만날 수 있어요."
    ),

    EXCHANGE_FINISHED(
            TRACKER_EXCHANGE_COMPLETED,
            "교환독서가 모두 끝났어요",
            "{bookTitle}과 함께 한 시간, 어땠었나요? 잊어버리기 전 후기를 남겨주세요!"
    ),

    REVIEW_DONE_CONFIRMED(
            TRACKER_REVIEW_DONE_CONFIRMED,
            "파트너가 후기를 작성했어요!",
            "{nickname}님이 {bookTitle}에 대한 후기 작성을 완료했어요."
    );

    private final NotificationType notificationType;
    public final String title;
    public final String bodyTemplate;
}
