package com.example.bookiibookii.domain.notification.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.repository.MeetingRepository;
import com.example.bookiibookii.domain.notification.entity.Notification;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.event.DirectExchangeNotificationEvent;
import com.example.bookiibookii.domain.notification.util.NotificationFactory;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.resolver.ActiveExchangeRoundResolver;
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

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectExchangeNotificationServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-20T05:00:00Z");

    @Mock
    private NotificationStore notificationStore;
    @Mock
    private UserRepository userRepository;
    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MatchedMemberRepository matchedMemberRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private DirectExchangeNotificationService service;

    @BeforeEach
    void setUp() {
        lenient().when(userRepository.getReferenceById(any())).thenAnswer(invocation ->
                User.builder().id(invocation.getArgument(0)).build());
        service = new DirectExchangeNotificationService(
                notificationStore,
                new NotificationFactory(userRepository, objectMapper),
                meetingRepository,
                matchedMemberRepository,
                new ActiveExchangeRoundResolver(),
                Clock.fixed(NOW, ZoneId.of("UTC"))
        );
    }

    @Test
    void storesMeetingChangeWithEventSpecificDedupKeyAndDirectPayload() throws Exception {
        Instant meetingAt = Instant.parse("2026-06-20T05:00:00Z");
        UUID eventId = UUID.randomUUID();
        service.send(new DirectExchangeNotificationEvent(
                NotificationType.DIRECT_MEETING_UPDATED,
                1L,
                "호스트",
                2L,
                10L,
                100L,
                ExchangeRound.RETURN_EXCHANGE,
                meetingAt,
                eventId,
                "교환 책"
        ));

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore).save(captor.capture());
        Notification notification = captor.getValue();
        JsonNode payload = objectMapper.readTree(notification.getPayload());

        assertThat(notification.getTitle()).isEqualTo("교환 약속이 변경됐어요");
        assertThat(notification.getMessage())
                .isEqualTo("호스트님이 교환 약속을 변경했어요. 새 일시와 장소를 확인해주세요.");
        assertThat(notification.getDedupKey()).isEqualTo(
                "DIRECT_MEETING_UPDATED:group:10:round:RETURN_EXCHANGE:receiver:2:"
                        + "meeting:100:event:" + eventId
        );
        assertThat(notification.getType()).isEqualTo(NotificationType.DIRECT_MEETING_UPDATED);
        assertThat(payload.has("notificationCode")).isFalse();
        assertThat(payload.has("type")).isFalse();
        assertThat(payload.get("redirectType").asText()).isEqualTo("TRACKER_DETAIL");
        assertThat(payload.get("exchangeType").asText()).isEqualTo("DIRECT");
        assertThat(payload.get("exchangeRound").asText()).isEqualTo("RETURN_EXCHANGE");
    }

    @Test
    void repeatedProcessingOfSameMeetingUpdateUsesSameDedupKey() {
        UUID eventId = UUID.randomUUID();
        DirectExchangeNotificationEvent event = new DirectExchangeNotificationEvent(
                NotificationType.DIRECT_MEETING_UPDATED,
                1L,
                "호스트",
                2L,
                10L,
                100L,
                ExchangeRound.FIRST_EXCHANGE,
                Instant.parse("2026-06-20T05:00:00Z"),
                eventId,
                "교환 책"
        );

        service.send(event);
        service.send(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        org.mockito.Mockito.verify(notificationStore, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getDedupKey)
                .containsOnly(
                        "DIRECT_MEETING_UPDATED:group:10:round:FIRST_EXCHANGE:receiver:2:"
                                + "meeting:100:event:" + eventId
                );
    }

    @Test
    void separateMeetingUpdateEventsUseDifferentDedupKeysEvenForSameFinalState() {
        Instant meetingAt = Instant.parse("2026-06-20T05:00:00Z");
        DirectExchangeNotificationEvent first = meetingUpdateEvent(meetingAt, UUID.randomUUID());
        DirectExchangeNotificationEvent second = meetingUpdateEvent(meetingAt, UUID.randomUUID());

        service.send(first);
        service.send(second);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        org.mockito.Mockito.verify(notificationStore, org.mockito.Mockito.times(2)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(Notification::getDedupKey)
                .doesNotHaveDuplicates();
    }

    @Test
    void storesFirstRoundReminderWithRoundSpecificDedupKey() {
        Instant meetingAt = overdueMeetingAt();
        DirectExchangeNotificationEvent event = reminder(ExchangeRound.FIRST_EXCHANGE, meetingAt);
        stubCurrentReminderTarget(event, ReadingStatus.EXCHANGING, ExchangeStatus.MEETING_SCHEDULED);

        service.send(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore).save(captor.capture());
        assertThat(captor.getValue().getDedupKey()).isEqualTo(
                "DIRECT_MEETING_CONFIRM_REMINDER:group:10:round:FIRST_EXCHANGE:receiver:2:meetingAt:"
                        + meetingAt
        );
    }

    @Test
    void storesReturnRoundReminderWhenReturnRoundIsActive() {
        DirectExchangeNotificationEvent event = reminder(ExchangeRound.RETURN_EXCHANGE, overdueMeetingAt());
        stubCurrentReminderTarget(event, ReadingStatus.RETURNING, ExchangeStatus.MEETING_SCHEDULED);

        service.send(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationStore).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.DIRECT_MEETING_CONFIRM_REMINDER);
        assertThat(captor.getValue().getDedupKey()).contains("round:RETURN_EXCHANGE");
    }

    @Test
    void skipsFirstRoundReminderWhenReadingStatusChangedToReturnRound() {
        DirectExchangeNotificationEvent event = reminder(ExchangeRound.FIRST_EXCHANGE, overdueMeetingAt());
        stubCurrentReminderTarget(event, ReadingStatus.RETURNING, ExchangeStatus.MEETING_SCHEDULED);

        service.send(event);

        verify(notificationStore, never()).save(any());
    }

    @Test
    void skipsReminderWhenMeetingScheduleChangedAfterEventWasPublished() {
        DirectExchangeNotificationEvent event = reminder(ExchangeRound.FIRST_EXCHANGE, overdueMeetingAt());
        stubCurrentReminderTarget(
                event,
                ReadingStatus.EXCHANGING,
                ExchangeStatus.MEETING_SCHEDULED,
                event.meetingAt().plus(java.time.Duration.ofHours(1))
        );

        service.send(event);

        verify(notificationStore, never()).save(any());
    }

    @Test
    void skipsReminderWhenReceiverCompletedMeeting() {
        DirectExchangeNotificationEvent event = reminder(ExchangeRound.FIRST_EXCHANGE, overdueMeetingAt());
        stubCurrentReminderTarget(event, ReadingStatus.EXCHANGING, ExchangeStatus.MEETING_COMPLETED);

        service.send(event);

        verify(notificationStore, never()).save(any());
    }

    @Test
    void skipsReminderOutsideActiveMeetingStage() {
        DirectExchangeNotificationEvent event = reminder(ExchangeRound.FIRST_EXCHANGE, overdueMeetingAt());
        stubCurrentReminderTarget(event, ReadingStatus.PARTNER_BOOK_READING, ExchangeStatus.MEETING_SCHEDULED);

        service.send(event);

        verify(notificationStore, never()).save(any());
    }

    @Test
    void skipsSelfNotification() {
        service.send(new DirectExchangeNotificationEvent(
                NotificationType.DIRECT_MEETING_CREATED,
                1L,
                "호스트",
                1L,
                10L,
                100L,
                ExchangeRound.FIRST_EXCHANGE,
                Instant.parse("2026-06-20T05:00:00Z"),
                null,
                "교환 책"
        ));

        verify(notificationStore, never()).save(any());
    }

    private DirectExchangeNotificationEvent reminder(ExchangeRound exchangeRound, Instant meetingAt) {
        return new DirectExchangeNotificationEvent(
                NotificationType.DIRECT_MEETING_CONFIRM_REMINDER,
                null,
                null,
                2L,
                10L,
                100L,
                exchangeRound,
                meetingAt,
                null,
                "교환 책"
        );
    }

    private DirectExchangeNotificationEvent meetingUpdateEvent(Instant meetingAt, UUID eventId) {
        return new DirectExchangeNotificationEvent(
                NotificationType.DIRECT_MEETING_UPDATED,
                1L,
                "호스트",
                2L,
                10L,
                100L,
                ExchangeRound.FIRST_EXCHANGE,
                meetingAt,
                eventId,
                "교환 책"
        );
    }

    private void stubCurrentReminderTarget(
            DirectExchangeNotificationEvent event,
            ReadingStatus readingStatus,
            ExchangeStatus receiverExchangeStatus
    ) {
        stubCurrentReminderTarget(event, readingStatus, receiverExchangeStatus, event.meetingAt());
    }

    private void stubCurrentReminderTarget(
            DirectExchangeNotificationEvent event,
            ReadingStatus readingStatus,
            ExchangeStatus receiverExchangeStatus,
            Instant currentScheduledAt
    ) {
        Groups group = Groups.builder()
                .id(event.groupId())
                .tradeType(TradeType.DIRECT)
                .groupStatus(GroupStatus.MATCHED)
                .build();
        Meeting meeting = Meeting.builder()
                .group(group)
                .exchangeRound(event.exchangeRound())
                .meetingAt(currentScheduledAt)
                .build();
        List<MatchedMember> members = List.of(
                member(1L, group, readingStatus, ExchangeStatus.MEETING_SCHEDULED),
                member(event.receiverId(), group, readingStatus, receiverExchangeStatus)
        );
        when(meetingRepository.findByGroupIdAndExchangeRound(event.groupId(), event.exchangeRound()))
                .thenReturn(Optional.of(meeting));
        lenient().when(matchedMemberRepository.findAllByGroup_Id(event.groupId())).thenReturn(members);
    }

    private MatchedMember member(
            Long userId,
            Groups group,
            ReadingStatus readingStatus,
            ExchangeStatus exchangeStatus
    ) {
        return MatchedMember.builder()
                .group(group)
                .user(User.builder().id(userId).build())
                .readingStatus(readingStatus)
                .exchangeStatus(exchangeStatus)
                .build();
    }

    private Instant overdueMeetingAt() {
        return NOW.minusSeconds(2 * 60 * 60);
    }
}
