package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.dto.NotificationResDTO;
import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.exception.NotificationException;
import com.example.bookiibookii.domain.notification.exception.code.NotificationErrorCode;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceCompatibilityTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Test
    void returnsNewNotificationTypeAndPayloadWithoutChangingResponseShape() {
        Notification notification = Notification.builder()
                .id(1L)
                .receiver(User.builder().id(10L).build())
                .category(NotificationCategory.SYSTEM)
                .type(NotificationType.READING_CARD_REACTION_CREATED)
                .title("파트너가 반응을 남겼어요")
                .message("파트너님이 회원님의 독서카드에 반응을 남겼어요.")
                .payload("""
                        {"redirectType":"BOOK_CARD_DETAIL","cardId":20}
                        """)
                .read(false)
                .build();
        ReflectionTestUtils.setField(notification, "createdAt", Instant.parse("2026-06-12T01:00:00Z"));
        when(notificationRepository.findFirstPage(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq(NotificationCategory.SYSTEM),
                any(Pageable.class)
        )).thenReturn(List.of(notification));

        NotificationService service = new NotificationService(notificationRepository, new ObjectMapper(), Clock.systemUTC());
        NotificationResDTO.NotificationListRes response =
                service.getNotifications(10L, NotificationCategory.SYSTEM, null, 20);

        assertThat(response.items()).hasSize(1);
        NotificationResDTO.NotificationItemRes item = response.items().get(0);
        assertThat(item.type()).isEqualTo("READING_CARD_REACTION_CREATED");
        assertThat(item.payload())
                .containsEntry("redirectType", "BOOK_CARD_DETAIL")
                .containsEntry("cardId", 20)
                .doesNotContainKeys("type", "notificationType", "notificationCode");
    }

    @Test
    void rejectsCursorWhenReadTokenIsNotBooleanLiteral() {
        NotificationService service = new NotificationService(notificationRepository, new ObjectMapper(), Clock.systemUTC());

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
