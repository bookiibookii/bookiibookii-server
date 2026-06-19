package com.example.bookiibookii.domain.tracker.event;

import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;

public record DeliveryNotificationEvent(
        NotificationType notificationType,
        Long actorId,
        String actorNickname,
        Long receiverId,
        Long groupId,
        ExchangeRound exchangeRound,
        String deliveryId,
        String bookTitle
) {
}
