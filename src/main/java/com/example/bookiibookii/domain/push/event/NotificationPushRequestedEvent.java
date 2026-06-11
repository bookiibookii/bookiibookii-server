package com.example.bookiibookii.domain.push.event;

public record NotificationPushRequestedEvent(
        Long notificationId,
        Long receiverId,
        String notificationType,
        String title,
        String body,
        String payload
) {
}
