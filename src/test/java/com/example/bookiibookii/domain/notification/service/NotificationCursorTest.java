package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.exception.NotificationException;
import com.example.bookiibookii.domain.notification.exception.code.NotificationErrorCode;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class NotificationCursorTest {

    @Test
    void rejectsCursorWhenReadTokenIsNotBooleanLiteral() {
        NotificationService service = new NotificationService(
                mock(NotificationRepository.class),
                new ObjectMapper(),
                Clock.systemUTC()
        );

        assertThatThrownBy(() -> service.getNotifications(
                10L,
                NotificationCategory.SYSTEM,
                "yes_2026-06-12T01:00:00Z_1",
                20
        ))
                .isInstanceOf(NotificationException.class)
                .extracting("code")
                .isEqualTo(NotificationErrorCode.INVALID_CURSOR);
    }
}
