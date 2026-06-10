package com.example.bookiibookii.domain.tracker.event;

import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;

import java.time.LocalDate;
import java.util.List;

public record TrackerNotificationEvent(
        NotificationType notificationType,
        Long actorId,
        Long actorMatchedMemberId,
        String actorNickname,
        List<Long> receiverIds,
        Long groupId,
        ExchangeType exchangeType,
        String bookTitle,
        Long bookId,
        Long commentId,
        Long reviewId,
        ExchangeRound exchangeRound,
        LocalDate periodEnd
) {
}
