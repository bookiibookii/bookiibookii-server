package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.service.NotificationStore;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
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

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrackerNotificationServiceTest {

    @Mock
    private NotificationStore notificationStore;
    @Mock
    private UserRepository userRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TrackerNotificationService service;

    @BeforeEach
    void setUp() {
        when(userRepository.getReferenceById(any())).thenAnswer(invocation ->
                User.builder().id(invocation.getArgument(0)).build());
        service = new TrackerNotificationService(
                notificationStore,
                new NotificationFactory(userRepository, objectMapper)
        );
    }

    @Test
    void storesBookReviewCompletionWithRoundSpecificDedupKey() throws Exception {
        service.send(event(
                NotificationType.TRACKER_READING_REVIEW_COMPLETED,
                List.of(2L),
                null,
                null,
                ExchangeRound.FIRST_EXCHANGE,
                null
        ));

        Notification notification = captureOne();
        JsonNode payload = objectMapper.readTree(notification.getPayload());
        assertThat(notification.getReceiver().getId()).isEqualTo(2L);
        assertThat(notification.getTitle()).isEqualTo("파트너가 책 교환 준비를 마쳤어요");
        assertThat(notification.getMessage()).isEqualTo("작성자님이 교환 책 독서를 마치고 후기를 남겼어요.");
        assertThat(notification.getActor().getId()).isEqualTo(1L);
        assertThat(notification.getDedupKey())
                .isEqualTo("TRACKER_READING_REVIEW_COMPLETED:2:10:1:100:FIRST_EXCHANGE");
        assertThat(payload.get("redirectType").asText()).isEqualTo("TRACKER_DETAIL");
        assertThat(payload.get("groupId").asLong()).isEqualTo(10L);
        assertThat(payload.get("exchangeType").asText()).isEqualTo("DELIVERY");
        assertThat(payload.get("actorId").asLong()).isEqualTo(1L);
        assertThat(payload.get("matchedMemberId").asLong()).isEqualTo(100L);
        assertThat(payload.get("exchangeRound").asText()).isEqualTo("FIRST_EXCHANGE");
        assertThat(payload.get("bookId").asLong()).isEqualTo(200L);
    }

    @Test
    void storesChangedPeriodForGuestWithChangeSpecificDedupKey() {
        service.send(event(
                NotificationType.TRACKER_PERIOD_EXTENDED,
                List.of(2L),
                null,
                null,
                null,
                LocalDate.of(2026, 6, 30)
        ));

        Notification notification = captureOne();
        assertThat(notification.getReceiver().getId()).isEqualTo(2L);
        assertThat(notification.getDedupKey()).isEqualTo("TRACKER_PERIOD_EXTENDED:2:10:2026-06-30");
    }

    @Test
    void storesTrackerCommentWithoutSendingToWriter() throws Exception {
        service.send(event(
                NotificationType.TRACKER_COMMENT_CREATED,
                List.of(1L, 2L),
                300L,
                null,
                null,
                null
        ));

        Notification notification = captureOne();
        JsonNode payload = objectMapper.readTree(notification.getPayload());
        assertThat(notification.getReceiver().getId()).isEqualTo(2L);
        assertThat(notification.getDedupKey()).isEqualTo("TRACKER_COMMENT_CREATED:2:10:300");
        assertThat(payload.get("redirectType").asText()).isEqualTo("TRACKER_COMMENT");
        assertThat(payload.get("commentId").asLong()).isEqualTo(300L);
    }

    @Test
    void storesFirstFinalReviewOnlyForPartner() throws Exception {
        service.send(event(
                NotificationType.TRACKER_EXCHANGE_REVIEW_CREATED,
                List.of(2L),
                null,
                400L,
                null,
                null
        ));

        Notification notification = captureOne();
        JsonNode payload = objectMapper.readTree(notification.getPayload());
        assertThat(notification.getReceiver().getId()).isEqualTo(2L);
        assertThat(notification.getDedupKey()).isEqualTo("TRACKER_EXCHANGE_REVIEW_CREATED:2:10:400");
        assertThat(payload.get("reviewId").asLong()).isEqualTo(400L);
    }

    @Test
    void storesCompletionForBothMembersWithoutFirstReviewNotification() throws Exception {
        service.send(event(
                NotificationType.TRACKER_EXCHANGE_COMPLETED,
                List.of(1L, 2L),
                null,
                null,
                null,
                null
        ));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getType)
                .containsOnly(NotificationType.TRACKER_EXCHANGE_COMPLETED);
        assertThat(captor.getAllValues())
                .extracting(Notification::getDedupKey)
                .containsExactly("TRACKER_EXCHANGE_COMPLETED:1:10", "TRACKER_EXCHANGE_COMPLETED:2:10");
        JsonNode payload = objectMapper.readTree(captor.getAllValues().get(0).getPayload());
        assertThat(payload.get("redirectType").asText()).isEqualTo("TRACKER_HOME");
        assertThat(payload.get("groupId").asLong()).isEqualTo(10L);
    }

    @Test
    void retriedEventUsesSameDedupKey() {
        TrackerNotificationEvent event = event(
                NotificationType.TRACKER_COMMENT_CREATED,
                List.of(2L),
                300L,
                null,
                null,
                null
        );

        service.send(event);
        service.send(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore, times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getDedupKey)
                .containsExactly("TRACKER_COMMENT_CREATED:2:10:300", "TRACKER_COMMENT_CREATED:2:10:300");
    }

    @Test
    void bookReviewDedupKeySeparatesRoundAndActor() {
        service.send(event(
                NotificationType.TRACKER_READING_REVIEW_COMPLETED,
                List.of(2L),
                null,
                null,
                ExchangeRound.FIRST_EXCHANGE,
                null
        ));
        service.send(event(
                NotificationType.TRACKER_READING_REVIEW_COMPLETED,
                List.of(2L),
                null,
                null,
                ExchangeRound.RETURN_EXCHANGE,
                null
        ));
        service.send(new TrackerNotificationEvent(
                NotificationType.TRACKER_READING_REVIEW_COMPLETED,
                2L,
                200L,
                "상대",
                List.of(1L),
                10L,
                ExchangeType.DELIVERY,
                "교환 책",
                200L,
                null,
                null,
                ExchangeRound.FIRST_EXCHANGE,
                null
        ));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore, times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getDedupKey)
                .containsExactly(
                        "TRACKER_READING_REVIEW_COMPLETED:2:10:1:100:FIRST_EXCHANGE",
                        "TRACKER_READING_REVIEW_COMPLETED:2:10:1:100:RETURN_EXCHANGE",
                        "TRACKER_READING_REVIEW_COMPLETED:1:10:2:200:FIRST_EXCHANGE"
                );
    }

    private TrackerNotificationEvent event(
            NotificationType type,
            List<Long> receiverIds,
            Long commentId,
            Long reviewId,
            ExchangeRound exchangeRound,
            LocalDate periodEnd
    ) {
        return new TrackerNotificationEvent(
                type,
                1L,
                100L,
                "작성자",
                receiverIds,
                10L,
                ExchangeType.DELIVERY,
                "교환 책",
                200L,
                commentId,
                reviewId,
                exchangeRound,
                periodEnd
        );
    }

    private Notification captureOne() {
        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore).save(captor.capture());
        return captor.getValue();
    }
}
