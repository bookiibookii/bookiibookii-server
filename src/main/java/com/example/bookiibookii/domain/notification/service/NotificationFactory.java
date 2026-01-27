package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationFactory {

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // converter
    public Notification create(Long receiverId, NotificationType type,
                               String title, String message, String payload) {
        User receiver = userRepository.getReferenceById(receiverId);
        return Notification.builder()
                .receiver(receiver)
                .type(type)
                .title(title)
                .message(message)
                .payload(payload)
                .read(false)
                .build();
    }

    // payload 필드 위한 json 생성
    public String toJson(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize notification payload", e);
        }
    }
}

