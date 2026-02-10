package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.converter.NotificationConverter;
import com.example.bookiibookii.domain.notification.dto.NotificationResDTO;
import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.exception.code.NotificationErrorCode;
import com.example.bookiibookii.domain.notification.exception.NotificationException;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final NotificationConverter notificationConverter;

    public NotificationResDTO.NotificationListRes getNotifications(
            Long receiverId,
            NotificationCategory category,
            String cursor,
            int size
    ) {
        int pageSize = clampSize(size);
        var pageable = PageRequest.of(0, pageSize + 1);

        Cursor parsed = parseCursor(cursor);

        List<Notification> fetched = (parsed == null)
                ? notificationRepository.findFirstPage(receiverId, category, pageable)
                : notificationRepository.findNextPage(receiverId, category, parsed.read, parsed.createdAt, parsed.id, pageable);

        boolean hasNext = fetched.size() > pageSize;
        List<Notification> page = hasNext ? fetched.subList(0, pageSize) : fetched;

        String nextCursor = null;
        if (hasNext && !page.isEmpty()) {
            Notification last = page.get(page.size() - 1);
            nextCursor = buildCursor(last.isRead(), last.getCreatedAt(), last.getId());
        }

        return notificationConverter.toNotificationListRes(page, nextCursor, hasNext);
    }

    @Transactional
    public NotificationResDTO.NotificationReadRes markAsRead(Long receiverId, Long notificationId) {

        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!n.getReceiver().getId().equals(receiverId)) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_FORBIDDEN);
        }

        n.markAsRead();
        return notificationConverter.toNotificationReadRes(n);
    }

    private int clampSize(int size) {
        if (size <= 0) return 20;
        return Math.min(size, 50);
    }

    //  cursor utils
    private record Cursor(boolean read, LocalDateTime createdAt, Long id) {
    }

    private Cursor parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) return null;

        // format: {read}_{createdAt}_{id}
        int first = cursor.indexOf('_');
        int last = cursor.lastIndexOf('_');
        if (first < 0 || last <= first) {
            throw new NotificationException(NotificationErrorCode.INVALID_CURSOR);
        }

        try {
            boolean read = Boolean.parseBoolean(cursor.substring(0, first));
            LocalDateTime createdAt = LocalDateTime.parse(cursor.substring(first + 1, last));
            Long id = Long.parseLong(cursor.substring(last + 1));
            return new Cursor(read, createdAt, id);
        } catch (Exception e) {
            throw new NotificationException(NotificationErrorCode.INVALID_CURSOR);
        }
    }

    private String buildCursor(boolean read, LocalDateTime createdAt, Long id) {
        return read + "_" + createdAt + "_" + id;
    }
}

