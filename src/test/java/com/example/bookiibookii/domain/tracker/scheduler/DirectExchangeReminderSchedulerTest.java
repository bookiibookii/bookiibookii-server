package com.example.bookiibookii.domain.tracker.scheduler;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.repository.MeetingRepository;
import com.example.bookiibookii.domain.notification.event.DirectExchangeNotificationEvent;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.resolver.ActiveExchangeRoundResolver;
import com.example.bookiibookii.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DirectExchangeReminderSchedulerTest {

    private static final Instant NOW = Instant.parse("2026-06-20T05:00:00Z");

    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private MatchedMemberRepository matchedMemberRepository;
    @Mock
    private DomainEventPublisher eventPublisher;

    @Test
    void publishesOnlyForMemberWhoHasNotCompletedCurrentMeeting() {
        DirectExchangeReminderScheduler scheduler = scheduler();
        Groups group = Groups.builder()
                .id(10L)
                .book(Book.builder().title("교환 책").build())
                .build();
        Meeting meeting = Meeting.builder()
                .group(group)
                .exchangeRound(ExchangeRound.FIRST_EXCHANGE)
                .meetingAt(NOW.minusSeconds(2 * 60 * 60))
                .build();
        MatchedMember incomplete = member(
                1L, group, ReadingStatus.EXCHANGING, ExchangeStatus.MEETING_SCHEDULED
        );
        MatchedMember completed = member(
                2L, group, ReadingStatus.EXCHANGING, ExchangeStatus.MEETING_COMPLETED
        );

        when(meetingRepository.findDueDirectMeetingReminders(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(List.of(meeting));
        when(matchedMemberRepository.findAllByGroup_Id(group.getId()))
                .thenReturn(List.of(incomplete, completed));

        scheduler.sendOverdueMeetingReminders();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        DirectExchangeNotificationEvent event = (DirectExchangeNotificationEvent) captor.getValue();
        assertThat(event.notificationType()).isEqualTo(NotificationType.DIRECT_MEETING_CONFIRM_REMINDER);
        assertThat(event.receiverId()).isEqualTo(incomplete.getUser().getId());
    }

    @Test
    void publishesForBothMembersWhenNeitherHasCompleted() {
        DirectExchangeReminderScheduler scheduler = scheduler();
        Groups group = group();
        Meeting meeting = meeting(group);
        MatchedMember first = member(1L, group, ReadingStatus.EXCHANGING, ExchangeStatus.MEETING_SCHEDULED);
        MatchedMember second = member(2L, group, ReadingStatus.EXCHANGING, ExchangeStatus.MEETING_SCHEDULED);
        stubDueMeeting(meeting, List.of(first, second));

        scheduler.sendOverdueMeetingReminders();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publish(captor.capture());
        assertThat(captor.getAllValues())
                .map(DirectExchangeNotificationEvent.class::cast)
                .extracting(DirectExchangeNotificationEvent::receiverId)
                .containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    void publishesNothingWhenBothMembersCompleted() {
        DirectExchangeReminderScheduler scheduler = scheduler();
        Groups group = group();
        Meeting meeting = meeting(group);
        stubDueMeeting(meeting, List.of(
                member(1L, group, ExchangeStatus.MEETING_COMPLETED),
                member(2L, group, ExchangeStatus.MEETING_COMPLETED)
        ));

        scheduler.sendOverdueMeetingReminders();

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void publishesNothingWhenRepositoryExcludesEndedGroupOrNotYetDueMeeting() {
        DirectExchangeReminderScheduler scheduler = scheduler();
        when(meetingRepository.findDueDirectMeetingReminders(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(List.of());

        scheduler.sendOverdueMeetingReminders();

        verify(eventPublisher, never()).publish(any());
        verify(matchedMemberRepository, never()).findAllByGroup_Id(any());
    }

    @Test
    void queriesOnlyMatchedDirectMeetingsAtLeastOneHourOverdue() {
        DirectExchangeReminderScheduler scheduler = scheduler();
        when(meetingRepository.findDueDirectMeetingReminders(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(List.of());

        scheduler.sendOverdueMeetingReminders();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(meetingRepository).findDueDirectMeetingReminders(
                cutoffCaptor.capture(),
                eq(TradeType.DIRECT),
                eq(GroupStatus.MATCHED),
                eq(ExchangeStatus.MEETING_SCHEDULED),
                eq(ExchangeRound.FIRST_EXCHANGE),
                eq(ReadingStatus.EXCHANGING),
                eq(ExchangeRound.RETURN_EXCHANGE),
                eq(ReadingStatus.RETURNING)
        );
        assertThat(cutoffCaptor.getValue()).isEqualTo(NOW.minusSeconds(60 * 60));
    }

    @Test
    void publishesFirstRoundReminderOnlyForFirstRoundMeeting() {
        Groups group = group();
        Meeting first = meeting(group, ExchangeRound.FIRST_EXCHANGE);
        Meeting second = meeting(group, ExchangeRound.RETURN_EXCHANGE);
        List<MatchedMember> members = List.of(
                member(1L, group, ReadingStatus.EXCHANGING, ExchangeStatus.MEETING_SCHEDULED),
                member(2L, group, ReadingStatus.EXCHANGING, ExchangeStatus.MEETING_SCHEDULED)
        );
        stubDueMeetings(List.of(first, second), members);

        scheduler().sendOverdueMeetingReminders();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publish(captor.capture());
        assertThat(captor.getAllValues())
                .map(DirectExchangeNotificationEvent.class::cast)
                .extracting(DirectExchangeNotificationEvent::exchangeRound)
                .containsOnly(ExchangeRound.FIRST_EXCHANGE);
    }

    @Test
    void publishesReturnRoundReminderButNeverPastFirstRoundMeeting() {
        Groups group = group();
        Meeting first = meeting(group, ExchangeRound.FIRST_EXCHANGE);
        Meeting second = meeting(group, ExchangeRound.RETURN_EXCHANGE);
        List<MatchedMember> members = List.of(
                member(1L, group, ReadingStatus.RETURNING, ExchangeStatus.MEETING_SCHEDULED),
                member(2L, group, ReadingStatus.RETURNING, ExchangeStatus.MEETING_SCHEDULED)
        );
        stubDueMeetings(List.of(first, second), members);

        scheduler().sendOverdueMeetingReminders();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publish(captor.capture());
        assertThat(captor.getAllValues())
                .map(DirectExchangeNotificationEvent.class::cast)
                .extracting(DirectExchangeNotificationEvent::exchangeRound)
                .containsOnly(ExchangeRound.RETURN_EXCHANGE);
    }

    @Test
    void publishesNothingOutsideActiveMeetingStages() {
        for (ReadingStatus status : List.of(
                ReadingStatus.MY_BOOK_READING,
                ReadingStatus.PARTNER_BOOK_READING,
                ReadingStatus.PARTNER_BOOK_REVIEWING,
                ReadingStatus.PARTNER_REVIEWING,
                ReadingStatus.COMPLETED
        )) {
            Groups group = group();
            stubDueMeetings(
                    List.of(
                            meeting(group, ExchangeRound.FIRST_EXCHANGE),
                            meeting(group, ExchangeRound.RETURN_EXCHANGE)
                    ),
                    List.of(
                            member(1L, group, status, ExchangeStatus.MEETING_SCHEDULED),
                            member(2L, group, status, ExchangeStatus.MEETING_SCHEDULED)
                    )
            );

            scheduler().sendOverdueMeetingReminders();
        }

        verify(eventPublisher, never()).publish(any());
    }

    private DirectExchangeReminderScheduler scheduler() {
        return new DirectExchangeReminderScheduler(
                meetingRepository,
                matchedMemberRepository,
                eventPublisher,
                new ActiveExchangeRoundResolver(),
                Clock.fixed(NOW, ZoneId.of("UTC"))
        );
    }

    private Groups group() {
        return Groups.builder()
                .id(10L)
                .book(Book.builder().title("교환 책").build())
                .build();
    }

    private Meeting meeting(Groups group) {
        return meeting(group, ExchangeRound.FIRST_EXCHANGE);
    }

    private Meeting meeting(Groups group, ExchangeRound exchangeRound) {
        return Meeting.builder()
                .group(group)
                .exchangeRound(exchangeRound)
                .meetingAt(NOW.minusSeconds(2 * 60 * 60))
                .build();
    }

    private void stubDueMeeting(Meeting meeting, List<MatchedMember> members) {
        stubDueMeetings(List.of(meeting), members);
    }

    private void stubDueMeetings(List<Meeting> meetings, List<MatchedMember> members) {
        when(meetingRepository.findDueDirectMeetingReminders(
                any(), any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(meetings);
        when(matchedMemberRepository.findAllByGroup_Id(meetings.get(0).getGroup().getId()))
                .thenReturn(members);
    }

    private MatchedMember member(Long userId, Groups group, ExchangeStatus exchangeStatus) {
        return member(userId, group, ReadingStatus.EXCHANGING, exchangeStatus);
    }

    private MatchedMember member(
            Long userId,
            Groups group,
            ReadingStatus readingStatus,
            ExchangeStatus exchangeStatus
    ) {
        return MatchedMember.builder()
                .id(userId)
                .group(group)
                .user(User.builder().id(userId).build())
                .readingStatus(readingStatus)
                .exchangeStatus(exchangeStatus)
                .build();
    }
}
