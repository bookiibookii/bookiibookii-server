package com.example.bookiibookii.domain.tracker.enums;

import java.time.format.DateTimeFormatter;
import java.util.Map;

public enum TrackerNotiType {

    READING_STARTED(
            "파트너가 책을 읽기 시작했어요!",
            "{nick}님이 {bookTitle} 읽기를 시작했습니다."
    ),

    EXTEND_REQUESTED(
            "독서 기간이 조금 늘어났어요",
            "{nick}님이 책을 더 깊이 읽기 위해 독서 기간을 연장했어요. (반납 예정일: {returnDueAt})"
    ),

    READING_FINISHED(
            "파트너가 책을 다 읽었어요",
            null
    ),

    SHIPPING_REGISTERED(
            "소중한 책이 오고 있어요!",
            "{nick}님이 책을 보내셨어요. 운송장 번호를 확인하고 배송을 조회해보세요."
    ),

    RECEIVED_CONFIRMED(
            "책이 무사히 도착했대요!",
            "{nick}님이 책을 잘 받으셨다고 해요. 수령 인증을 확인해볼까요?"
    ),

    RETURN_SHIPPING_REGISTERED(
            "책이 집으로 돌아오고 있어요",
            "{nick}님이 독서를 마치고 책을 반납했어요. 곧 소중한 책을 다시 만날 수 있어요."
    ),

    EXCHANGE_FINISHED(
            "교환독서가 모두 끝났어요",
            "{bookTitle}과 함께 한 시간, 어땠었나요? 잊어버리기 전 후기를 남겨주세요!"
    );

    private static final DateTimeFormatter DUE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    public final String title;
    public final String bodyTemplate;

    TrackerNotiType(String title, String bodyTemplate) {
        this.title = title;
        this.bodyTemplate = bodyTemplate;
    }

    public String renderBody(Map<String, String> vars) {
        if (bodyTemplate == null) return null;
        String result = bodyTemplate;
        for (var entry : vars.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", safe(entry.getValue()));
        }
        return result;
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
