package com.example.bookiibookii.domain.tracker.scheduler;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.notification.repository.NotificationRepository;
import com.example.bookiibookii.domain.tracker.entity.Delivery;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.event.DeliveryNotificationEvent;
import com.example.bookiibookii.domain.tracker.repository.DeliveryRepository;
import com.example.bookiibookii.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.repository.Query;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryReceiveReminderSchedulerTest {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final Instant NOW = Instant.parse("2026-06-19T03:00:00Z");

    @Mock
    private DeliveryRepository deliveryRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private DomainEventPublisher eventPublisher;

    @Test
    void queriesDeliveryCreatedAtCutoffAtExactly72Hours() {
        when(deliveryRepository.findReceiveReminderCandidates(any())).thenReturn(List.of());

        scheduler().sendReceiveReminders();

        ArgumentCaptor<Instant> cutoff = ArgumentCaptor.forClass(Instant.class);
        verify(deliveryRepository).findReceiveReminderCandidates(cutoff.capture());
        assertThat(cutoff.getValue()).isEqualTo(Instant.parse("2026-06-16T03:00:00Z"));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void publishesReminderForDueUnconfirmedDelivery() {
        Delivery delivery = delivery();
        when(deliveryRepository.findReceiveReminderCandidates(any())).thenReturn(List.of(delivery));
        when(notificationRepository.existsByReceiver_IdAndDedupKey(2L,
                "delivery:receive-reminder:group:10:round:FIRST_EXCHANGE:delivery:delivery-1"
        )).thenReturn(false);

        scheduler().sendReceiveReminders();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        DeliveryNotificationEvent event = (DeliveryNotificationEvent) captor.getValue();
        assertThat(event.notificationType()).isEqualTo(NotificationType.TRACKER_DELIVERY_RECEIVE_REMINDER);
        assertThat(event.receiverId()).isEqualTo(2L);
        assertThat(event.actorId()).isEqualTo(1L);
        assertThat(event.bookTitle()).isEqualTo("발송한 책");
    }

    @Test
    void doesNotPublishReminderTwice() {
        Delivery delivery = delivery();
        when(deliveryRepository.findReceiveReminderCandidates(any())).thenReturn(List.of(delivery));
        when(notificationRepository.existsByReceiver_IdAndDedupKey(any(), any())).thenReturn(true);

        scheduler().sendReceiveReminders();

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void continuesWithNextCandidateWhenOneCandidateFails() {
        Delivery failedDelivery = delivery("delivery-1", 10L, 2L);
        Delivery successfulDelivery = delivery("delivery-2", 20L, 3L);
        when(deliveryRepository.findReceiveReminderCandidates(any()))
                .thenReturn(List.of(failedDelivery, successfulDelivery));
        doThrow(new IllegalStateException("publish failed"))
                .doNothing()
                .when(eventPublisher).publish(any());

        scheduler().sendReceiveReminders();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publish(captor.capture());
        assertThat(captor.getAllValues())
                .map(DeliveryNotificationEvent.class::cast)
                .extracting(DeliveryNotificationEvent::deliveryId)
                .containsExactly("delivery-1", "delivery-2");
    }

    @Test
    void addressCreationTimeCannotMakeReminderEligible() {
        Query query = reminderQuery();

        assertThat(query.value()).contains("d.trackingRegisteredAt <= :cutoff");
        assertThat(query.value()).doesNotContain("DeliveryAddress", "updatedAt");
    }

    @Test
    void receivedDeliveryIsNotReturnedAsReminderCandidate() {
        assertThat(reminderQuery().value()).contains("d.receivedConfirmedAt is null");
    }

    @Test
    void deliveryStatusUpdateNotificationTypeIsNotImplemented() {
        assertThat(NotificationType.values())
                .noneMatch(type -> type.name().contains("DELIVERY_STATUS_UPDATED"));
    }

    private DeliveryReceiveReminderScheduler scheduler() {
        return new DeliveryReceiveReminderScheduler(
                deliveryRepository,
                notificationRepository,
                eventPublisher,
                Clock.fixed(NOW, KST)
        );
    }

    private Query reminderQuery() {
        try {
            return DeliveryRepository.class
                    .getMethod("findReceiveReminderCandidates", Instant.class)
                    .getAnnotation(Query.class);
        } catch (NoSuchMethodException exception) {
            throw new AssertionError(exception);
        }
    }

    private Delivery delivery() {
        return delivery("delivery-1", 10L, 2L);
    }

    private Delivery delivery(String deliveryId, Long groupId, Long receiverId) {
        Groups group = Groups.builder().id(groupId).build();
        MatchedMember sender = MatchedMember.builder()
                .id(100L)
                .group(group)
                .user(User.builder().id(1L).nickName("발송자").build())
                .build();
        sender.getMemberBooks().add(MemberBook.builder()
                .matchedMember(sender)
                .book(Book.builder().title("발송한 책").build())
                .isMine(true)
                .build());
        MatchedMember receiver = MatchedMember.builder()
                .id(200L)
                .group(group)
                .user(User.builder().id(receiverId).build())
                .build();
        return Delivery.builder()
                .id(deliveryId)
                .group(group)
                .exchangeRound(ExchangeRound.FIRST_EXCHANGE)
                .sender(sender)
                .receiver(receiver)
                .build();
    }
}
