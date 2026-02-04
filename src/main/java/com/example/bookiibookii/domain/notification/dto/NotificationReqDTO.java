package com.example.bookiibookii.domain.notification.dto;

import com.example.bookiibookii.domain.notification.enums.NotificationType;

public class NotificationReqDTO {

    public record NotificationListQuery(
            NotificationType type,
            String cursor, // nullable
            Integer size   // nullable
    ) {}
}
