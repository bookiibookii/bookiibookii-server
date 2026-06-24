package com.example.bookiibookii.domain.notification.util;

import com.example.bookiibookii.domain.notification.dto.NotificationPayload;
import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.exception.NotificationException;
import com.example.bookiibookii.domain.notification.exception.code.NotificationErrorCode;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationFactory {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // converter
    public Notification create(Long receiverId, NotificationCategory category, NotificationType type,
                               String title, String message, String payload) {
        return create(receiverId, category, type, title, message, payload, null);
    }

    public Notification create(Long receiverId, NotificationCategory category, NotificationType type,
                               String title, String message, String payload, String dedupKey) {
        return create(receiverId, null, category, type, title, message, payload, dedupKey);
    }

    public Notification create(Long receiverId, Long actorId, NotificationCategory category, NotificationType type,
                               String title, String message, String payload, String dedupKey) {
        User receiver = userRepository.getReferenceById(receiverId);
        return Notification.builder()
                .receiver(receiver)
                .actor(actorId == null ? null : userRepository.getReferenceById(actorId))
                .category(category)
                .type(type)
                .title(title)
                .message(message)
                .payload(payload)
                .dedupKey(dedupKey)
                .read(false)
                .build();
    }

    public String toJson(NotificationPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new NotificationException(NotificationErrorCode.NOTIFICATION_SERIALIZE_PAYLOAD_FAILED);
        }
    }
}
