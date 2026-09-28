package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.example.bookiibookii.domain.push.event.NotificationPushRequestedEvent;
import com.example.bookiibookii.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPersistenceServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Test
    void publishesPushRequestOnlyAfterNotificationIsSaved() {
        User receiver = User.builder().id(9L).build();
        Notification notification = Notification.builder()
                .receiver(receiver)
                .category(NotificationCategory.SYSTEM)
                .type(NotificationType.GROUP_COMMENT_CREATED)
                .title("title")
                .message("body")
                .payload("{\"groupId\":1}")
                .build();
        Notification saved = Notification.builder()
                .id(11L)
                .receiver(receiver)
                .category(NotificationCategory.SYSTEM)
                .type(NotificationType.GROUP_COMMENT_CREATED)
                .title("title")
                .message("body")
                .payload("{\"groupId\":1}")
                .build();
        when(notificationRepository.saveAndFlush(notification)).thenReturn(saved);
        NotificationPersistenceService service =
                new NotificationPersistenceService(notificationRepository, eventPublisher);

        service.saveAndFlush(notification);

        ArgumentCaptor<NotificationPushRequestedEvent> captor =
                ArgumentCaptor.forClass(NotificationPushRequestedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().notificationId()).isEqualTo(11L);
        assertThat(captor.getValue().receiverId()).isEqualTo(9L);
        assertThat(captor.getValue().title()).isEqualTo("title");
        assertThat(captor.getValue().body()).isEqualTo("body");
    }
}
