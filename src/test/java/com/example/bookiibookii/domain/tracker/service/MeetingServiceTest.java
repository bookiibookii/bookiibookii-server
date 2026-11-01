package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.repository.GroupPlaceRepository;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.repository.MeetingRepository;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.notification.event.DirectExchangeNotificationEvent;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.tracker.dto.req.MeetingRequestDTO;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;
    @Mock
    private GroupsRepository groupsRepository;
    @Mock
    private MatchedMemberRepository matchedMemberRepository;
    @Mock
    private GroupPlaceRepository groupPlaceRepository;
    @Mock
    private DomainEventPublisher eventPublisher;
    @Mock
    private Clock clock;

    @InjectMocks
    private MeetingService meetingService;

    @BeforeEach
    void setUpClock() {
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-06-20T05:00:00Z"));
    }

    @Test
    void createMeetingPublishesRegistrationNotificationForPartner() {
        Groups group = directGroup(9L);
        MatchedMember host = meetingMember(1L, 11L, group, RoleStatus.HOST);
        MatchedMember guest = meetingMember(2L, 22L, group, RoleStatus.GUEST);
        host.updateExchangeStatus(ExchangeStatus.MEETING_SCHEDULE_WAITING);
        guest.updateExchangeStatus(ExchangeStatus.MEETING_SCHEDULE_WAITING);
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 6, 20, 14, 0);
        MeetingRequestDTO request = request("카페", scheduledAt);

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.existsByGroupIdAndExchangeRound(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(false);
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = meetingService.createMeeting(group.getId(), request, host.getUser());

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        DirectExchangeNotificationEvent event = (DirectExchangeNotificationEvent) captor.getValue();
        assertThat(event.notificationType()).isEqualTo(
                com.example.bookiibookii.domain.notification.enums.NotificationType.DIRECT_MEETING_CREATED
        );
        assertThat(event.receiverId()).isEqualTo(guest.getUser().getId());
        assertThat(event.receiverId()).isNotEqualTo(event.actorId());
        assertThat(event.exchangeRound()).isEqualTo(ExchangeRound.FIRST_EXCHANGE);
        assertThat(response.meetingAt()).isEqualTo(toInstant(scheduledAt));
        assertThat(host.getExchangeStatus()).isEqualTo(ExchangeStatus.MEETING_SCHEDULED);
        assertThat(guest.getExchangeStatus()).isEqualTo(ExchangeStatus.MEETING_SCHEDULED);
    }

    @Test
    void createReturnMeetingPublishesRegistrationNotificationForPartner() {
        Groups group = directGroup(12L);
        MatchedMember host = meetingMember(5L, 55L, group, RoleStatus.HOST);
        MatchedMember guest = meetingMember(6L, 66L, group, RoleStatus.GUEST);
        host.updateReadingStatus(ReadingStatus.RETURNING);
        guest.updateReadingStatus(ReadingStatus.RETURNING);
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 7, 1, 18, 0);

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.existsByGroupIdAndExchangeRound(group.getId(), ExchangeRound.RETURN_EXCHANGE))
                .thenReturn(false);
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        meetingService.createMeeting(group.getId(), request("카페", scheduledAt), host.getUser());

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        DirectExchangeNotificationEvent event = (DirectExchangeNotificationEvent) captor.getValue();
        assertThat(event.exchangeRound()).isEqualTo(ExchangeRound.RETURN_EXCHANGE);
        assertThat(event.receiverId()).isEqualTo(guest.getUser().getId());
    }

    @Test
    void createMeetingDoesNotPublishWhenMeetingAlreadyExists() {
        Groups group = directGroup(13L);
        MatchedMember host = meetingMember(7L, 77L, group, RoleStatus.HOST);
        MatchedMember guest = meetingMember(8L, 88L, group, RoleStatus.GUEST);

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.existsByGroupIdAndExchangeRound(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(true);

        assertThatThrownBy(() ->
                meetingService.createMeeting(
                        group.getId(),
                        request("카페", LocalDateTime.of(2026, 7, 1, 18, 0)),
                        host.getUser()
                )
        ).isInstanceOf(com.example.bookiibookii.domain.tracker.exception.TrackerException.class);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void createMeetingSucceedsWhenMeetingRowDoesNotExistEvenIfMembersAreAlreadyScheduled() {
        Groups group = directGroup(18L);
        MatchedMember host = meetingMember(17L, 177L, group, RoleStatus.HOST);
        MatchedMember guest = meetingMember(18L, 188L, group, RoleStatus.GUEST);

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.existsByGroupIdAndExchangeRound(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(false);
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        meetingService.createMeeting(
                group.getId(),
                request("카페", LocalDateTime.of(2026, 7, 2, 18, 0)),
                host.getUser()
        );

        verify(meetingRepository).save(any(Meeting.class));
        verify(eventPublisher).publish(any());
    }

    @Test
    void createMeetingFailsWhenMeetingRowExistsForSameGroupAndExchangeRound() {
        Groups group = directGroup(19L);
        MatchedMember host = meetingMember(19L, 199L, group, RoleStatus.HOST);
        MatchedMember guest = meetingMember(20L, 200L, group, RoleStatus.GUEST);

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.existsByGroupIdAndExchangeRound(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(true);

        assertThatThrownBy(() -> meetingService.createMeeting(
                group.getId(),
                request("카페", LocalDateTime.of(2026, 7, 3, 18, 0)),
                host.getUser()
        )).isInstanceOf(com.example.bookiibookii.domain.tracker.exception.TrackerException.class);
        verify(meetingRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void createMeetingSucceedsWhenSameGroupHasDifferentExchangeRoundMeeting() {
        Groups group = directGroup(20L);
        MatchedMember host = meetingMember(21L, 211L, group, RoleStatus.HOST);
        MatchedMember guest = meetingMember(22L, 222L, group, RoleStatus.GUEST);
        host.updateReadingStatus(ReadingStatus.RETURNING);
        guest.updateReadingStatus(ReadingStatus.RETURNING);

        lenient().when(meetingRepository.existsByGroupIdAndExchangeRound(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(true);
        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.existsByGroupIdAndExchangeRound(group.getId(), ExchangeRound.RETURN_EXCHANGE))
                .thenReturn(false);
        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> invocation.getArgument(0));

        meetingService.createMeeting(
                group.getId(),
                request("카페", LocalDateTime.of(2026, 7, 4, 18, 0)),
                host.getUser()
        );

        verify(meetingRepository).existsByGroupIdAndExchangeRound(group.getId(), ExchangeRound.RETURN_EXCHANGE);
        verify(meetingRepository, never()).existsByGroupIdAndExchangeRound(group.getId(), ExchangeRound.FIRST_EXCHANGE);
        verify(meetingRepository).save(any(Meeting.class));
    }

    @Test
    void updateMeetingPublishesChangeNotificationWhenOnlyPlaceChanges() {
        Groups group = directGroup(10L);
        MatchedMember host = meetingMember(1L, 11L, group, RoleStatus.HOST);
        MatchedMember guest = meetingMember(2L, 22L, group, RoleStatus.GUEST);
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 6, 20, 14, 0);
        Meeting meeting = meeting(group, host, ExchangeRound.FIRST_EXCHANGE, scheduledAt);
        MeetingRequestDTO request = request("다른 카페", scheduledAt);

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.findByGroupIdAndExchangeRoundForUpdate(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(Optional.of(meeting));

        meetingService.updateMeeting(group.getId(), request, host.getUser());

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        DirectExchangeNotificationEvent event = (DirectExchangeNotificationEvent) captor.getValue();
        assertThat(event.receiverId()).isEqualTo(guest.getUser().getId());
        assertThat(event.meetingAt()).isEqualTo(toInstant(scheduledAt));
        assertThat(event.meetingId()).isEqualTo(meeting.getId());
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    void updateMeetingPublishesChangeNotificationWhenOnlyTimeChanges() {
        Groups group = directGroup(14L);
        MatchedMember host = meetingMember(9L, 99L, group, RoleStatus.HOST);
        MatchedMember guest = meetingMember(10L, 100L, group, RoleStatus.GUEST);
        LocalDateTime original = LocalDateTime.of(2026, 6, 20, 14, 0);
        LocalDateTime changed = original.plus(java.time.Duration.ofHours(1));
        Meeting meeting = meeting(group, host, ExchangeRound.FIRST_EXCHANGE, original);

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.findByGroupIdAndExchangeRoundForUpdate(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(Optional.of(meeting));

        var response = meetingService.updateMeeting(group.getId(), request("카페", changed), host.getUser());

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        DirectExchangeNotificationEvent event = (DirectExchangeNotificationEvent) captor.getValue();
        assertThat(event.meetingAt()).isEqualTo(toInstant(changed));
        assertThat(response.meetingAt()).isEqualTo(toInstant(changed));
    }

    @Test
    void getMeetingReturnsStoredInstantMeetingAt() {
        Groups group = directGroup(21L);
        MatchedMember host = meetingMember(23L, 233L, group, RoleStatus.HOST);
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 6, 24, 18, 0);
        Meeting meeting = meeting(group, host, ExchangeRound.FIRST_EXCHANGE, scheduledAt);

        when(groupsRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(group.getId(), host.getUser().getId()))
                .thenReturn(Optional.of(host));
        when(meetingRepository.findByGroupIdAndExchangeRound(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(Optional.of(meeting));

        var response = meetingService.getMeeting(group.getId(), host.getUser());

        assertThat(response.meetingAt()).isEqualTo(Instant.parse("2026-06-24T09:00:00Z"));
    }

    @Test
    void updateMeetingDoesNotPublishWhenScheduleAndPlaceAreUnchanged() {
        Groups group = directGroup(11L);
        MatchedMember host = meetingMember(3L, 33L, group, RoleStatus.HOST);
        MatchedMember guest = meetingMember(4L, 44L, group, RoleStatus.GUEST);
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 6, 20, 14, 0);
        Meeting meeting = meeting(group, host, ExchangeRound.FIRST_EXCHANGE, scheduledAt);
        MeetingRequestDTO request = request("카페", scheduledAt);

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.findByGroupIdAndExchangeRoundForUpdate(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(Optional.of(meeting));

        meetingService.updateMeeting(group.getId(), request, host.getUser());

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void updateMeetingPublishesAgainWhenPlaceChangesFromAToBAndBackToA() {
        Groups group = directGroup(15L);
        MatchedMember host = meetingMember(11L, 111L, group, RoleStatus.HOST);
        MatchedMember guest = meetingMember(12L, 122L, group, RoleStatus.GUEST);
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 6, 20, 14, 0);
        Meeting meeting = meeting(group, host, ExchangeRound.FIRST_EXCHANGE, scheduledAt);

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.findByGroupIdAndExchangeRoundForUpdate(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(Optional.of(meeting));

        meetingService.updateMeeting(group.getId(), request("다른 카페", scheduledAt), host.getUser());
        meetingService.updateMeeting(group.getId(), request("카페", scheduledAt), host.getUser());

        org.mockito.ArgumentCaptor<Object> captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        org.mockito.Mockito.verify(eventPublisher, org.mockito.Mockito.times(2)).publish(captor.capture());
        List<DirectExchangeNotificationEvent> events = captor.getAllValues().stream()
                .map(DirectExchangeNotificationEvent.class::cast)
                .toList();
        assertThat(events)
                .extracting(DirectExchangeNotificationEvent::eventId)
                .doesNotHaveDuplicates()
                .doesNotContainNull();
        assertThat(events)
                .extracting(DirectExchangeNotificationEvent::meetingId)
                .containsOnly(meeting.getId());
        assertThat(meeting.getPlaceName()).isEqualTo("카페");
    }

    @Test
    void updateMeetingIgnoresSurroundingWhitespaceAndStoresNormalizedStrings() {
        Groups group = directGroup(16L);
        MatchedMember host = meetingMember(13L, 133L, group, RoleStatus.HOST);
        MatchedMember guest = meetingMember(14L, 144L, group, RoleStatus.GUEST);
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 6, 20, 14, 0);
        Meeting meeting = meeting(group, host, ExchangeRound.FIRST_EXCHANGE, scheduledAt);
        MeetingRequestDTO request = new MeetingRequestDTO(
                "  카페  ",
                "  서울시 강남구  ",
                "  12345  ",
                BigDecimal.ONE,
                BigDecimal.ONE,
                "  2층  ",
                scheduledAt.atOffset(ZoneOffset.ofHours(9))
        );

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.findByGroupIdAndExchangeRoundForUpdate(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(Optional.of(meeting));

        meetingService.updateMeeting(group.getId(), request, host.getUser());

        verify(eventPublisher, never()).publish(any());
        assertThat(meeting.getPlaceName()).isEqualTo("카페");
        assertThat(meeting.getAddress()).isEqualTo("서울시 강남구");
        assertThat(meeting.getZipCode()).isEqualTo("12345");
        assertThat(meeting.getAddressDetail()).isEqualTo("2층");
    }

    @Test
    void updateMeetingStoresTrimmedStringsWhenLocationActuallyChanges() {
        Groups group = directGroup(17L);
        MatchedMember host = meetingMember(15L, 155L, group, RoleStatus.HOST);
        MatchedMember guest = meetingMember(16L, 166L, group, RoleStatus.GUEST);
        LocalDateTime scheduledAt = LocalDateTime.of(2026, 6, 20, 14, 0);
        Meeting meeting = meeting(group, host, ExchangeRound.FIRST_EXCHANGE, scheduledAt);
        MeetingRequestDTO request = new MeetingRequestDTO(
                "  강남역  ",
                "  서울시 강남구 강남대로  ",
                "  06232  ",
                BigDecimal.TEN,
                BigDecimal.TEN,
                "  11번 출구  ",
                scheduledAt.atOffset(ZoneOffset.ofHours(9))
        );

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(host, guest));
        when(meetingRepository.findByGroupIdAndExchangeRoundForUpdate(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(Optional.of(meeting));

        meetingService.updateMeeting(group.getId(), request, host.getUser());

        verify(eventPublisher).publish(any());
        assertThat(meeting.getPlaceName()).isEqualTo("강남역");
        assertThat(meeting.getAddress()).isEqualTo("서울시 강남구 강남대로");
        assertThat(meeting.getZipCode()).isEqualTo("06232");
        assertThat(meeting.getAddressDetail()).isEqualTo("11번 출구");
    }

    @Test
    void finalReturnMeetingCompletionKeepsTrackerOpenForMemberReview() {
        Groups group = Groups.builder()
                .id(1L)
                .tradeType(TradeType.DIRECT)
                .groupStatus(GroupStatus.MATCHED)
                .build();
        MatchedMember me = member(1L, 10L, group, ExchangeStatus.MEETING_SCHEDULED);
        MatchedMember partner = member(2L, 20L, group, ExchangeStatus.MEETING_COMPLETED);
        addBooks(me, "내 책", "상대 책");
        addBooks(partner, "상대 책", "내 책");
        me.changeCurrentMemberBook(findBook(me, false), Instant.now());
        partner.changeCurrentMemberBook(findBook(partner, false), Instant.now());
        findBook(me, true).updateCurrentPage(87);
        findBook(partner, true).updateCurrentPage(93);
        Meeting meeting = meeting(group, me);

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(me, partner));
        when(meetingRepository.findByGroupIdAndExchangeRoundForUpdate(group.getId(), ExchangeRound.RETURN_EXCHANGE))
                .thenReturn(Optional.of(meeting));

        meetingService.completeMeeting(group.getId(), me.getUser());

        assertThat(group.getGroupStatus()).isEqualTo(GroupStatus.MATCHED);
        assertThat(me.getReadingStatus()).isEqualTo(ReadingStatus.PARTNER_REVIEWING);
        assertThat(partner.getReadingStatus()).isEqualTo(ReadingStatus.PARTNER_REVIEWING);
        assertThat(me.getExchangeStatus()).isEqualTo(ExchangeStatus.NOT_STARTED);
        assertThat(partner.getExchangeStatus()).isEqualTo(ExchangeStatus.NOT_STARTED);
        assertThat(me.getCompletedAt()).isNull();
        assertThat(partner.getCompletedAt()).isNull();
        assertThat(me.isReviewWritten()).isFalse();
        assertThat(partner.isReviewWritten()).isFalse();
        assertThat(me.getPartnerReviewingStartedAt()).isEqualTo(Instant.parse("2026-06-20T05:00:00Z"));
        assertThat(partner.getPartnerReviewingStartedAt()).isEqualTo(Instant.parse("2026-06-20T05:00:00Z"));
        assertThat(me.getCurrentMemberBook().getBook().getTitle()).isEqualTo("내 책");
        assertThat(partner.getCurrentMemberBook().getBook().getTitle()).isEqualTo("상대 책");
        assertThat(me.getCurrentMemberBook().getCurrentPage()).isEqualTo(87);
        assertThat(partner.getCurrentMemberBook().getCurrentPage()).isEqualTo(93);
    }

    @Test
    void firstMeetingCompletionAdvancesBothMembersToPartnerBookReading() {
        Groups group = Groups.builder()
                .id(2L)
                .tradeType(TradeType.DIRECT)
                .groupStatus(GroupStatus.MATCHED)
                .build();
        MatchedMember me = member(3L, 30L, group, ReadingStatus.EXCHANGING, ExchangeStatus.MEETING_SCHEDULED);
        MatchedMember partner = member(4L, 40L, group, ReadingStatus.EXCHANGING, ExchangeStatus.MEETING_COMPLETED);
        addBooks(me, "내 책", "상대 책");
        addBooks(partner, "상대 책", "내 책");
        findBook(me, false).updateCurrentPage(100);
        findBook(partner, false).updateCurrentPage(100);
        Meeting meeting = Meeting.builder()
                .id(2L)
                .group(group)
                .createdBy(me)
                .exchangeRound(ExchangeRound.FIRST_EXCHANGE)
                .placeName("카페")
                .address("서울시 강남구")
                .zipCode("12345")
                .x(BigDecimal.ONE)
                .y(BigDecimal.ONE)
                .meetingAt(Instant.now())
                .build();

        when(groupsRepository.findByIdForUpdate(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(group.getId())).thenReturn(List.of(me, partner));
        when(meetingRepository.findByGroupIdAndExchangeRoundForUpdate(group.getId(), ExchangeRound.FIRST_EXCHANGE))
                .thenReturn(Optional.of(meeting));

        meetingService.completeMeeting(group.getId(), me.getUser());

        assertThat(me.getReadingStatus()).isEqualTo(ReadingStatus.PARTNER_BOOK_READING);
        assertThat(partner.getReadingStatus()).isEqualTo(ReadingStatus.PARTNER_BOOK_READING);
        assertThat(me.getExchangeStatus()).isEqualTo(ExchangeStatus.NOT_STARTED);
        assertThat(partner.getExchangeStatus()).isEqualTo(ExchangeStatus.NOT_STARTED);
        assertThat(me.getCurrentMemberBook().getBook().getTitle()).isEqualTo("상대 책");
        assertThat(partner.getCurrentMemberBook().getBook().getTitle()).isEqualTo("내 책");
        assertThat(me.getCurrentMemberBook().getCurrentPage()).isZero();
        assertThat(partner.getCurrentMemberBook().getCurrentPage()).isZero();
    }

    private MatchedMember member(Long memberId, Long userId, Groups group, ExchangeStatus exchangeStatus) {
        return member(memberId, userId, group, ReadingStatus.RETURNING, exchangeStatus);
    }

    private MatchedMember member(
            Long memberId,
            Long userId,
            Groups group,
            ReadingStatus readingStatus,
            ExchangeStatus exchangeStatus
    ) {
        return MatchedMember.builder()
                .id(memberId)
                .group(group)
                .user(User.builder().id(userId).build())
                .readingStatus(readingStatus)
                .exchangeStatus(exchangeStatus)
                .build();
    }

    private void addBooks(MatchedMember member, String myTitle, String partnerTitle) {
        member.getMemberBooks().add(MemberBook.builder()
                .matchedMember(member)
                .book(Book.builder().title(myTitle).build())
                .isMine(true)
                .build());
        member.getMemberBooks().add(MemberBook.builder()
                .matchedMember(member)
                .book(Book.builder().title(partnerTitle).build())
                .isMine(false)
                .build());
    }

    private MemberBook findBook(MatchedMember member, boolean isMine) {
        return member.getMemberBooks().stream()
                .filter(memberBook -> memberBook.isMine() == isMine)
                .findFirst()
                .orElseThrow();
    }

    private Meeting meeting(Groups group, MatchedMember createdBy) {
        return meeting(
                group,
                createdBy,
                ExchangeRound.RETURN_EXCHANGE,
                Instant.now()
        );
    }

    private Meeting meeting(
            Groups group,
            MatchedMember createdBy,
            ExchangeRound exchangeRound,
            Instant scheduledAt
    ) {
        return Meeting.builder()
                .id(1L)
                .group(group)
                .createdBy(createdBy)
                .exchangeRound(exchangeRound)
                .placeName("카페")
                .address("서울시 강남구")
                .zipCode("12345")
                .x(BigDecimal.ONE)
                .y(BigDecimal.ONE)
                .addressDetail("2층")
                .meetingAt(scheduledAt)
                .build();
    }

    private Meeting meeting(
            Groups group,
            MatchedMember createdBy,
            ExchangeRound exchangeRound,
            LocalDateTime scheduledAt
    ) {
        return meeting(group, createdBy, exchangeRound, toInstant(scheduledAt));
    }

    private Groups directGroup(Long id) {
        return Groups.builder()
                .id(id)
                .book(Book.builder().id(100L).title("교환 책").build())
                .tradeType(TradeType.DIRECT)
                .groupStatus(GroupStatus.MATCHED)
                .build();
    }

    private MatchedMember meetingMember(
            Long memberId,
            Long userId,
            Groups group,
            RoleStatus role
    ) {
        return MatchedMember.builder()
                .id(memberId)
                .group(group)
                .user(User.builder().id(userId).nickName("사용자" + userId).build())
                .role(role)
                .readingStatus(ReadingStatus.EXCHANGING)
                .exchangeStatus(ExchangeStatus.MEETING_SCHEDULED)
                .build();
    }

    private MeetingRequestDTO request(String placeName, LocalDateTime scheduledAt) {
        return new MeetingRequestDTO(
                placeName,
                "서울시 강남구",
                "12345",
                BigDecimal.ONE,
                BigDecimal.ONE,
                "2층",
                scheduledAt.atOffset(ZoneOffset.ofHours(9))
        );
    }

    private Instant toInstant(LocalDateTime meetingAt) {
        return meetingAt.atOffset(ZoneOffset.ofHours(9)).toInstant();
    }
}
