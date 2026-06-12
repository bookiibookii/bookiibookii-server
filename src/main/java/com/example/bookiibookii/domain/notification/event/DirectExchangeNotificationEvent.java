package com.example.bookiibookii.domain.notification.event;

import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;

import java.time.LocalDateTime;
import java.util.UUID;

public record DirectExchangeNotificationEvent(
        NotificationType notificationType,
        Long actorId,
        String actorNickname,
        Long receiverId,
        Long groupId,
        Long meetingId,
        ExchangeRound exchangeRound,
        LocalDateTime meetingAt,
        UUID eventId,
        String bookTitle
) {
}
