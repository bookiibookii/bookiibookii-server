package com.example.bookiibookii.domain.notification.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NotificationTest {

    @Test
    void markAsReadRejectsNullReadAtBeforeChangingState() {
        Notification notification = Notification.builder()
                .read(false)
                .build();

        assertThatThrownBy(() -> notification.markAsRead(null))
                .isInstanceOf(NullPointerException.class);
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getReadAt()).isNull();
    }

    @Test
    void markAsReadStoresReadAt() {
        Notification notification = Notification.builder()
                .read(false)
                .build();
        Instant readAt = Instant.parse("2026-06-20T05:00:00Z");

        notification.markAsRead(readAt);

        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isEqualTo(readAt);
    }
}
