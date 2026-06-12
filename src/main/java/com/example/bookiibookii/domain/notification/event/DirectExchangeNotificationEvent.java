package com.example.bookiibookii.domain.notification.event;

import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;

import java.time.LocalDateTime;

public record DirectExchangeNotificationEvent(
        NotificationType notificationType,
        Long actorId,
        String actorNickname,
        Long receiverId,
        Long groupId,
        ExchangeRound exchangeRound,
        LocalDateTime meetingAt,
        String placeHash,
        String bookTitle
) {
}
