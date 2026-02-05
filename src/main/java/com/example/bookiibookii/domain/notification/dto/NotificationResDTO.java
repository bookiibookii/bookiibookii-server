package com.example.bookiibookii.domain.notification.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class NotificationResDTO {

    public record NotificationItemRes(
            Long id,
            String type,
            String title,
            String message,
            boolean isRead,
            LocalDateTime createdAt,
            Map<String, Object> payload
    ) {}

    public record NotificationListRes(
            List<NotificationItemRes> items,
            String nextCursor,
            boolean hasNext
    ) {}

    public record NotificationReadRes(
            Long id,
            String type,
            boolean isRead,
            LocalDateTime readAt,
            Map<String, Object> payload
    ) {}
}
