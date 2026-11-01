package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.NotificationCategory;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.example.bookiibookii.domain.push.dto.PushMessage;
import com.example.bookiibookii.domain.push.listener.NotificationPushEventListener;
import com.example.bookiibookii.domain.push.service.PushService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({NotificationPersistenceService.class, NotificationStore.class,
        NotificationPushEventListener.class, NotificationBatchPersistenceIntegrationTest.Config.class})
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationBatchPersistenceIntegrationTest {

    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private NotificationStore notificationStore;
    @MockitoBean private PushService pushService;

    @BeforeEach
    void cleanUp() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void storesNewBatchAndPushesEachCommittedNotificationOnce() {
        User receiver = saveUser();

        List<Notification> saved = notificationStore.saveAll(List.of(
                notification(receiver, "FIRST"), notification(receiver, "LAST")));

        assertThat(saved).extracting(Notification::getDedupKey).containsExactly("FIRST", "LAST");
        assertThat(notificationRepository.findAll()).extracting(Notification::getId)
                .containsExactlyInAnyOrderElementsOf(saved.stream().map(Notification::getId).toList());
        assertThat(saved).allSatisfy(notification -> {
            assertThat(notification.isRead()).isFalse();
            assertThat(notification.getReadAt()).isNull();
            assertThat(notification.getActor()).isNull();
        });
        assertPushes(receiver, saved);
    }

    @Test
    void preservesNewNotificationsAroundDuplicateAndDoesNotPushDuplicateOnRetry() {
        User receiver = saveUser();
        Instant readAt = Instant.parse("2026-11-01T00:00:00Z");
        Notification original = notification(receiver, "DUPLICATE");
        original.markAsRead(readAt);
        Notification existing = notificationRepository.saveAndFlush(original);
        Notification first = notification(receiver, "FIRST", receiver);
        first.markAsRead(readAt);

        // IDENTITY assigns an ID to FIRST before the middle insert fails and rolls back the batch.
        List<Notification> saved = notificationStore.saveAll(List.of(
                first,
                notification(receiver, "DUPLICATE"),
                notification(receiver, "LAST")));

        assertThat(saved).extracting(Notification::getDedupKey).containsExactly("FIRST", "LAST");
        assertThat(notificationRepository.findAll()).extracting(Notification::getDedupKey)
                .containsExactlyInAnyOrder("FIRST", "DUPLICATE", "LAST");
        Notification unchanged = notificationRepository.findById(existing.getId()).orElseThrow();
        assertThat(unchanged.isRead()).isTrue();
        assertThat(unchanged.getReadAt()).isEqualTo(readAt);
        Notification retriedFirst = notificationRepository.findById(saved.get(0).getId()).orElseThrow();
        assertThat(retriedFirst.getActor().getId()).isEqualTo(receiver.getId());
        assertThat(retriedFirst.isRead()).isTrue();
        assertThat(retriedFirst.getReadAt()).isEqualTo(readAt);
        assertThat(saved.get(1).getActor()).isNull();
        assertThat(saved.get(1).isRead()).isFalse();
        assertThat(saved.get(1).getReadAt()).isNull();

        assertThat(notificationStore.saveAll(List.of(
                notification(receiver, "FIRST"),
                notification(receiver, "DUPLICATE"),
                notification(receiver, "LAST")))).isEmpty();
        assertThat(notificationRepository.count()).isEqualTo(3);
        assertPushes(receiver, saved);
    }

    @Test
    void emptyBatchDoesNotPersistOrPush() {
        assertThat(notificationStore.saveAll(List.of())).isEmpty();

        assertThat(notificationRepository.count()).isZero();
        verifyNoInteractions(pushService);
    }

    @Test
    void unrelatedIntegrityViolationRollsBackWholeBatchWithoutPush() {
        User receiver = saveUser();
        Notification invalid = Notification.builder()
                .receiver(receiver)
                .category(NotificationCategory.KEYWORD)
                .type(NotificationType.KEYWORD_GROUP_CREATED)
                .dedupKey("INVALID")
                .build();

        assertThatThrownBy(() -> notificationStore.saveAll(List.of(
                notification(receiver, "FIRST"), invalid, notification(receiver, "LAST"))))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(notificationRepository.count()).isZero();
        verifyNoInteractions(pushService);
    }

    private void assertPushes(User receiver, List<Notification> saved) {
        ArgumentCaptor<PushMessage> captor = ArgumentCaptor.forClass(PushMessage.class);
        // Async execution is disabled in this JPA slice; the real AFTER_COMMIT listener runs synchronously.
        verify(pushService, times(2)).sendToUser(eq(receiver.getId()), captor.capture());
        verifyNoMoreInteractions(pushService);
        assertThat(captor.getAllValues()).extracting(message -> message.data().get("notificationId"))
                .containsExactlyElementsOf(saved.stream().map(n -> n.getId().toString()).toList());
        for (int i = 0; i < saved.size(); i++) {
            Notification persisted = notificationRepository.findById(saved.get(i).getId()).orElseThrow();
            assertThat(persisted.getReceiver().getId()).isEqualTo(receiver.getId());
            assertThat(persisted.getCategory()).isEqualTo(NotificationCategory.KEYWORD);
            assertThat(persisted.getType()).isEqualTo(NotificationType.KEYWORD_GROUP_CREATED);
            assertThat(persisted.getTitle()).isEqualTo(saved.get(i).getDedupKey());
            assertThat(persisted.getMessage()).isEqualTo("message");
            assertThat(persisted.getPayload()).isEqualTo("{\"groupId\":1}");
            PushMessage push = captor.getAllValues().get(i);
            assertThat(push.title()).isEqualTo(persisted.getTitle());
            assertThat(push.body()).isEqualTo(persisted.getMessage());
            assertThat(push.data()).containsEntry("groupId", "1")
                    .containsEntry("type", NotificationType.KEYWORD_GROUP_CREATED.name());
        }
    }

    private User saveUser() {
        return userRepository.saveAndFlush(User.builder()
                .socialType(SocialType.KAKAO).socialId("batch-receiver").build());
    }

    private Notification notification(User receiver, String dedupKey) {
        return notification(receiver, dedupKey, null);
    }

    private Notification notification(User receiver, String dedupKey, User actor) {
        return Notification.builder()
                .receiver(receiver)
                .actor(actor)
                .category(NotificationCategory.KEYWORD)
                .type(NotificationType.KEYWORD_GROUP_CREATED)
                .title(dedupKey)
                .message("message")
                .payload("{\"groupId\":1}")
                .dedupKey(dedupKey)
                .read(false)
                .build();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Config {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }
}
