package com.example.bookiibookii.domain.group.service;

import com.example.bookiibookii.domain.group.enums.GroupNotiType;
import com.example.bookiibookii.domain.group.event.GroupNotificationEvent;
import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.service.NotificationStore;
import com.example.bookiibookii.domain.notification.util.NotiTemplateRenderer;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GroupNotificationServiceTest {

    @Mock private NotificationStore notificationStore;
    @Mock private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GroupNotificationService service;

    @BeforeEach
    void setUp() {
        when(userRepository.getReferenceById(any())).thenAnswer(invocation ->
                User.builder().id(invocation.getArgument(0)).build());
        service = new GroupNotificationService(
                notificationStore,
                new NotificationFactory(userRepository, objectMapper),
                new NotiTemplateRenderer(),
                userRepository
        );
    }

    @Test
    void storesJoinRequestNotificationWithRequestPayload() throws Exception {
        when(userRepository.findNickNameById(10L)).thenReturn(Optional.of("게스트"));

        service.send(event(GroupNotiType.JOIN_REQUESTED, 10L, 20L, 100L, null));

        Notification notification = captureNotification();
        JsonNode payload = objectMapper.readTree(notification.getPayload());
        assertThat(notification.getType()).isEqualTo(NotificationType.GROUP_JOIN_REQUEST);
        assertThat(notification.getTitle()).isEqualTo("새로운 참여 요청이 왔어요");
        assertThat(payload.get("groupId").asLong()).isEqualTo(1L);
        assertThat(payload.get("requestId").asLong()).isEqualTo(100L);
        assertThat(notification.getDedupKey()).isEqualTo("NOTI-GRP-001:100");
    }

    @Test
    void storesAcceptedNotificationWithTrackerPayload() throws Exception {
        when(userRepository.findNickNameById(10L)).thenReturn(Optional.of("호스트"));

        service.send(event(GroupNotiType.MATCH_SUCCEEDED, 10L, 20L, 100L, ExchangeType.DELIVERY));

        Notification notification = captureNotification();
        JsonNode payload = objectMapper.readTree(notification.getPayload());
        assertThat(notification.getType()).isEqualTo(NotificationType.GROUP_REQUEST_ACCEPTED);
        assertThat(payload.get("redirectType").asText()).isEqualTo("TRACKER_DETAIL");
        assertThat(payload.get("requestId").asLong()).isEqualTo(100L);
        assertThat(payload.get("exchangeType").asText()).isEqualTo("DELIVERY");
        assertThat(notification.getDedupKey()).isEqualTo("NOTI-GRP-002:100");
    }

    @Test
    void storesRejectedNotificationWithRequestPayload() throws Exception {
        when(userRepository.findNickNameById(10L)).thenReturn(Optional.of("호스트"));

        service.send(event(GroupNotiType.MATCH_REJECTED, 10L, 20L, 100L, null));

        Notification notification = captureNotification();
        JsonNode payload = objectMapper.readTree(notification.getPayload());
        assertThat(notification.getType()).isEqualTo(NotificationType.GROUP_REQUEST_REJECTED);
        assertThat(payload.get("redirectType").asText()).isEqualTo("EXPLORE_HOME");
        assertThat(payload.get("requestId").asLong()).isEqualTo(100L);
        assertThat(notification.getDedupKey()).isEqualTo("NOTI-GRP-003:100");
    }

    private GroupNotificationEvent event(
            GroupNotiType type,
            Long actorId,
            Long receiverId,
            Long requestId,
            ExchangeType exchangeType
    ) {
        return new GroupNotificationEvent(
                type, actorId, "교환 독서", receiverId, null, 1L, requestId, exchangeType);
    }

    private Notification captureNotification() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore).save(captor.capture());
        return captor.getValue();
    }
}
