package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.entity.Meeting;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.group.repository.MeetingRepository;
import com.example.bookiibookii.domain.group.util.ReadingPeriodDateCalculator;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.tracker.dto.TrackerStepInfo;
import com.example.bookiibookii.domain.tracker.dto.req.ReadingProgressRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.ReadingProgressResponseDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerDetailResDTO;
import com.example.bookiibookii.domain.tracker.dto.res.TrackerListResDTO;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerStepStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerTopBannerType;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
import com.example.bookiibookii.domain.tracker.resolver.TrackerDisplayStatusResolver;
import com.example.bookiibookii.domain.tracker.resolver.TrackerDueDateResolver;
import com.example.bookiibookii.domain.tracker.resolver.TrackerPartnerResolver;
import com.example.bookiibookii.domain.tracker.resolver.TrackerStepAssembler;
import com.example.bookiibookii.domain.tracker.resolver.TrackerTopBannerResolver;
import com.example.bookiibookii.domain.tracker.resolver.UserProfileImageUrlResolver;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class TrackerServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 6, 8);

    @Mock
    private MatchedMemberRepository matchedMemberRepository;

    @Mock
    private GroupsRepository groupsRepository;

    @Mock
    private UserProfileImageUrlResolver userProfileImageUrlResolver;

    @Mock
    private TrackerStepAssembler trackerStepAssembler;

    @Mock
    private BookReviewRepository bookReviewRepository;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private DomainEventPublisher eventPublisher;

    private TrackerService trackerService;
    private TrackerDueDateResolver trackerDueDateResolver;

    @BeforeEach
    void setUp() {
        trackerDueDateResolver = new TrackerDueDateResolver(Clock.fixed(
                TODAY.atStartOfDay(ReadingPeriodDateCalculator.KST).toInstant(),
                ReadingPeriodDateCalculator.KST
        ));
        trackerService = new TrackerService(
                matchedMemberRepository,
                groupsRepository,
                new TrackerPartnerResolver(),
                new TrackerDisplayStatusResolver(),
                trackerDueDateResolver,
                userProfileImageUrlResolver,
                new TrackerStepAssembler(),
                new TrackerTopBannerResolver(Clock.fixed(
                        TODAY.atStartOfDay(ReadingPeriodDateCalculator.KST).toInstant(),
                        ReadingPeriodDateCalculator.KST
                )),
                meetingRepository,
                bookReviewRepository,
                eventPublisher
        );
    }

    @Test
    void getTrackerListIncludesMyRoleFromCurrentUsersMatchedMember() {
        User guest = user(1L, "guest");
        User host = user(2L, "host");
        Groups group = group();

        MatchedMember me = matchedMember(10L, group, guest, RoleStatus.GUEST);
        MatchedMember partner = matchedMember(20L, group, host, RoleStatus.HOST);
        MemberBook myBook = memberBook(100L, group, me, true, "나의 책");
        MemberBook partnerBook = memberBook(200L, group, partner, true, "상대 책");
        me.getMemberBooks().add(myBook);
        partner.getMemberBooks().add(partnerBook);
        ReflectionTestUtils.setField(me, "currentMemberBook", myBook);
        ReflectionTestUtils.setField(partner, "currentMemberBook", partnerBook);
        group.getMatchedMember().addAll(List.of(me, partner));

        when(matchedMemberRepository.findAllTrackerItemsByMemberId(
                guest.getId(),
                GroupStatus.COMPLETED,
                ReadingStatus.COMPLETED
        )).thenReturn(List.of(me));
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(me.getId(), myBook.getId()))
                .thenReturn(false);

        TrackerListResDTO response = trackerService.getTrackerList(guest);

        assertThat(response.nickname()).isEqualTo("guest");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).getMyRole()).isEqualTo(RoleStatus.GUEST);
        assertThat(response.items().get(0).getDisplayBookTitle()).isEqualTo("나의 책");
        assertThat(response.items().get(0).getDisplayStatusLabel()).isEqualTo("읽는 중");
        assertThat(response.items().get(0).getRemainingDays()).isEqualTo(6);
        assertThat(response.topBanners()).hasSize(1);
        assertThat(response.topBanners().get(0).bannerType())
                .isEqualTo(TrackerTopBannerType.READING_IN_PROGRESS);
        verify(meetingRepository, never()).findAllByGroupIds(any());
        verify(matchedMemberRepository).findAllTrackerItemsByMemberId(
                guest.getId(),
                GroupStatus.COMPLETED,
                ReadingStatus.COMPLETED
        );
    }

    @Test
    void getTrackerListLoadsDirectMeetingsWithSingleBatchQuery() {
        User guest = user(1L, "guest");
        User host = user(2L, "host");
        Groups group = Groups.builder()
                .id(2L)
                .groupName("직접 교환")
                .tradeType(TradeType.DIRECT)
                .startDate(TODAY)
                .readingPeriod(7)
                .matchedMember(new ArrayList<>())
                .build();
        MatchedMember me = matchedMember(10L, group, guest, RoleStatus.GUEST);
        MatchedMember partner = matchedMember(20L, group, host, RoleStatus.HOST);
        me.updateReadingStatus(ReadingStatus.EXCHANGING);
        partner.updateReadingStatus(ReadingStatus.EXCHANGING);
        me.updateExchangeStatus(ExchangeStatus.MEETING_SCHEDULED);
        partner.updateExchangeStatus(ExchangeStatus.MEETING_SCHEDULED);
        MemberBook myBook = memberBook(100L, group, me, true, "나의 책");
        MemberBook partnerBook = memberBook(200L, group, partner, true, "상대 책");
        ReflectionTestUtils.setField(me, "currentMemberBook", myBook);
        ReflectionTestUtils.setField(partner, "currentMemberBook", partnerBook);
        group.getMatchedMember().addAll(List.of(me, partner));
        Instant scheduledAt = TODAY.atTime(15, 0).atOffset(ZoneOffset.ofHours(9)).toInstant();
        Meeting meeting = Meeting.builder()
                .id(30L)
                .group(group)
                .createdBy(partner)
                .exchangeRound(com.example.bookiibookii.domain.tracker.enums.ExchangeRound.FIRST_EXCHANGE)
                .placeName("강남역")
                .address("서울")
                .x(BigDecimal.ZERO)
                .y(BigDecimal.ZERO)
                .meetingAt(scheduledAt)
                .build();

        when(matchedMemberRepository.findAllTrackerItemsByMemberId(
                guest.getId(),
                GroupStatus.COMPLETED,
                ReadingStatus.COMPLETED
        )).thenReturn(List.of(me));
        when(meetingRepository.findAllByGroupIds(List.of(group.getId()))).thenReturn(List.of(meeting));
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(me.getId(), myBook.getId()))
                .thenReturn(false);

        TrackerListResDTO response = trackerService.getTrackerList(guest);

        assertThat(response.topBanners()).hasSize(1);
        assertThat(response.topBanners().get(0).bannerType())
                .isEqualTo(TrackerTopBannerType.DIRECT_MEETING_SCHEDULED);
        assertThat(response.topBanners().get(0).partnerNickname()).isEqualTo("host");
        assertThat(response.topBanners().get(0).title())
                .isEqualTo("{nickname} 님과의 책 교환까지 {remainingTime} 남았어요.");
        assertThat(response.topBanners().get(0).titleTemplate())
                .isEqualTo("{nickname} 님과의 책 교환까지 {remainingTime} 남았어요.");
        assertThat(response.topBanners().get(0).dDayLabel()).isEqualTo("D-Day");
        assertThat(response.topBanners().get(0).targetAt()).isEqualTo(scheduledAt);
        assertThat(response.topBanners().get(0).remainingSeconds()).isEqualTo(54_000L);
        verify(meetingRepository).findAllByGroupIds(List.of(group.getId()));
    }

    @Test
    void updateReadingProgressChangesCurrentPage() {
        User user = user(1L, "reader");
        Groups group = group();
        MatchedMember me = matchedMember(10L, group, user, RoleStatus.GUEST);
        MemberBook currentBook = memberBook(100L, group, me, true, "나의 책");
        ReflectionTestUtils.setField(me, "currentMemberBook", currentBook);
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(group.getId(), user.getId()))
                .thenReturn(java.util.Optional.of(me));

        ReadingProgressResponseDTO response = trackerService.updateReadingProgress(
                group.getId(),
                new ReadingProgressRequestDTO(45),
                user
        );

        assertThat(currentBook.getCurrentPage()).isEqualTo(45);
        assertThat(response.currentPage()).isEqualTo(45);
        assertThat(response.progressRate()).isEqualTo(45);
    }

    @Test
    void finishingBookOnlyDoesNotPublishReviewCompletionNotification() {
        User user = user(1L, "reader");
        Groups group = group();
        MatchedMember me = matchedMember(10L, group, user, RoleStatus.GUEST);
        MemberBook currentBook = memberBook(100L, group, me, true, "나의 책");
        ReflectionTestUtils.setField(me, "currentMemberBook", currentBook);
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(group.getId(), user.getId()))
                .thenReturn(Optional.of(me));

        trackerService.updateReadingProgress(
                group.getId(),
                new ReadingProgressRequestDTO(100),
                user
        );

        assertThat(me.getReadingStatus()).isEqualTo(ReadingStatus.MY_BOOK_REVIEWING);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void finishingBookPublishesRoundCompletionWhenReviewAlreadyExists() {
        User user = user(1L, "reader");
        User partner = user(2L, "partner");
        Groups group = group();
        MatchedMember me = matchedMember(10L, group, user, RoleStatus.GUEST);
        MemberBook currentBook = memberBook(100L, group, me, true, "나의 책");
        ReflectionTestUtils.setField(me, "currentMemberBook", currentBook);
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(group.getId(), user.getId()))
                .thenReturn(Optional.of(me));
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(me.getId(), currentBook.getId()))
                .thenReturn(true);
        when(matchedMemberRepository.findPartnerUserId(group.getId(), user.getId()))
                .thenReturn(Optional.of(partner.getId()));

        trackerService.updateReadingProgress(
                group.getId(),
                new ReadingProgressRequestDTO(100),
                user
        );

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        TrackerNotificationEvent event = (TrackerNotificationEvent) captor.getValue();
        assertThat(event.notificationType()).isEqualTo(NotificationType.TRACKER_READING_REVIEW_COMPLETED);
        assertThat(event.receiverIds()).containsExactly(partner.getId());
        assertThat(event.exchangeRound()).isEqualTo(
                com.example.bookiibookii.domain.tracker.enums.ExchangeRound.FIRST_EXCHANGE
        );
    }

    @Test
    void changedReadingPeriodPublishesGuestNotification() {
        User host = user(1L, "host");
        User guest = user(2L, "guest");
        Groups group = group();
        MatchedMember guestMember = matchedMember(20L, group, guest, RoleStatus.GUEST);
        when(matchedMemberRepository.findRoleByGroupIdAndUserId(group.getId(), host.getId()))
                .thenReturn(Optional.of(RoleStatus.HOST));
        when(groupsRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(matchedMemberRepository.findByGroup_IdAndRole(group.getId(), RoleStatus.GUEST))
                .thenReturn(Optional.of(guestMember));

        trackerService.extendReadingPeriod(
                group.getId(),
                new com.example.bookiibookii.domain.tracker.dto.req.ExtendReadingPeriodReqDTO(TODAY.plusDays(9)),
                host
        );

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        TrackerNotificationEvent event = (TrackerNotificationEvent) captor.getValue();
        assertThat(event.notificationType()).isEqualTo(NotificationType.TRACKER_PERIOD_EXTENDED);
        assertThat(event.receiverIds()).containsExactly(guest.getId());
        assertThat(event.periodEnd()).isEqualTo(TODAY.plusDays(9));
    }

    @Test
    void unchangedReadingPeriodDoesNotPublishNotification() {
        User host = user(1L, "host");
        Groups group = group();
        when(matchedMemberRepository.findRoleByGroupIdAndUserId(group.getId(), host.getId()))
                .thenReturn(Optional.of(RoleStatus.HOST));
        when(groupsRepository.findById(group.getId())).thenReturn(Optional.of(group));

        trackerService.extendReadingPeriod(
                group.getId(),
                new com.example.bookiibookii.domain.tracker.dto.req.ExtendReadingPeriodReqDTO(TODAY.plusDays(6)),
                host
        );

        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void getTrackerDetailMarksPartnerBookReviewingStepCompletedAfterCurrentBookReviewWritten() {
        User guest = user(1L, "guest");
        User host = user(2L, "host");
        Groups group = group();

        MatchedMember me = matchedMember(10L, group, guest, RoleStatus.GUEST);
        MatchedMember partner = matchedMember(20L, group, host, RoleStatus.HOST);
        me.updateReadingStatus(ReadingStatus.PARTNER_BOOK_REVIEWING);
        me.updateExchangeStatus(ExchangeStatus.NOT_STARTED);
        partner.updateReadingStatus(ReadingStatus.PARTNER_BOOK_REVIEWING);
        partner.updateExchangeStatus(ExchangeStatus.NOT_STARTED);

        MemberBook myBook = memberBook(100L, group, me, true, "나의 책");
        MemberBook myPartnerBook = memberBook(101L, group, me, false, "내가 읽는 파트너 책");
        MemberBook partnerBook = memberBook(200L, group, partner, true, "상대 책");
        MemberBook partnerPartnerBook = memberBook(201L, group, partner, false, "상대가 읽는 책");
        me.getMemberBooks().addAll(List.of(myBook, myPartnerBook));
        partner.getMemberBooks().addAll(List.of(partnerBook, partnerPartnerBook));
        ReflectionTestUtils.setField(me, "currentMemberBook", myPartnerBook);
        ReflectionTestUtils.setField(partner, "currentMemberBook", partnerPartnerBook);

        when(matchedMemberRepository.findAllTrackerMembersByGroupId(group.getId())).thenReturn(List.of(me, partner));
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(me.getId(), myPartnerBook.getId()))
                .thenReturn(true);

        TrackerDetailResDTO response = trackerService.getTrackerDetail(group.getId(), guest);

        assertThat(response.displayStatus()).isEqualTo(TrackerDisplayStatus.REVIEW_WAITING_PARTNER);
        assertThat(step(response, TrackerStepStatus.PARTNER_BOOK_REVIEWING).completed()).isTrue();
        assertThat(step(response, TrackerStepStatus.RETURN_TRACKING_REGISTER).completed()).isFalse();
        assertThat(me.isReviewWritten()).isFalse();
    }

    @Test
    void finalExchangeReviewStatusIsDerivedPerLoggedInMember() {
        User guestUser = user(1L, "guest");
        User hostUser = user(2L, "host");
        Groups group = group();
        MatchedMember guest = matchedMember(10L, group, guestUser, RoleStatus.GUEST);
        MatchedMember host = matchedMember(20L, group, hostUser, RoleStatus.HOST);
        Instant partnerReviewingStartedAt = Instant.parse("2026-06-20T05:00:00Z");
        guest.updateReadingStatus(ReadingStatus.PARTNER_REVIEWING, partnerReviewingStartedAt);
        host.updateReadingStatus(ReadingStatus.PARTNER_REVIEWING, partnerReviewingStartedAt);
        guest.updateExchangeStatus(ExchangeStatus.NOT_STARTED);
        host.updateExchangeStatus(ExchangeStatus.NOT_STARTED);
        guest.markReviewAsWritten();

        MemberBook guestBook = memberBook(100L, group, guest, true, "게스트 책");
        MemberBook guestPartnerBook = memberBook(101L, group, guest, false, "호스트 책");
        MemberBook hostBook = memberBook(200L, group, host, true, "호스트 책");
        MemberBook hostPartnerBook = memberBook(201L, group, host, false, "게스트 책");
        guest.getMemberBooks().addAll(List.of(guestBook, guestPartnerBook));
        host.getMemberBooks().addAll(List.of(hostBook, hostPartnerBook));
        ReflectionTestUtils.setField(guest, "currentMemberBook", guestBook);
        ReflectionTestUtils.setField(host, "currentMemberBook", hostBook);
        guestBook.updateCurrentPage(87);
        hostBook.updateCurrentPage(93);

        when(matchedMemberRepository.findAllTrackerMembersByGroupId(group.getId()))
                .thenReturn(List.of(guest, host));
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(guest.getId(), guestBook.getId()))
                .thenReturn(true);
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(host.getId(), hostBook.getId()))
                .thenReturn(true);

        TrackerDetailResDTO guestView = trackerService.getTrackerDetail(group.getId(), guestUser);
        TrackerDetailResDTO hostView = trackerService.getTrackerDetail(group.getId(), hostUser);

        assertThat(guestView.displayStatus())
                .isEqualTo(TrackerDisplayStatus.EXCHANGE_REVIEW_WAITING_PARTNER);
        assertThat(guestView.displayStatusLabel()).isEqualTo("파트너 후기 대기");
        assertThat(step(guestView, TrackerStepStatus.PARTNER_REVIEWING).description())
                .isEqualTo("파트너가 교환독서 후기를 작성하면 교환독서가 종료돼요.");
        assertThat(hostView.displayStatus()).isEqualTo(TrackerDisplayStatus.EXCHANGE_REVIEW_WRITING);
        assertThat(hostView.displayStatusLabel()).isEqualTo("교환 후기 작성");

        assertThat(guestView.myBook().title()).isEqualTo("게스트 책");
        assertThat(guestView.partnerBook().title()).isEqualTo("호스트 책");
        assertThat(guestView.myBook().currentReaderNickname()).isEqualTo("guest");
        assertThat(guestView.partnerBook().currentReaderNickname()).isEqualTo("host");
        assertThat(guestView.myBook().currentPage()).isEqualTo(87);
        assertThat(hostView.myBook().currentPage()).isEqualTo(93);
    }

    @Test
    void completedTrackerDetailIsRejectedForBothMembers() {
        User guestUser = user(3L, "guest");
        User hostUser = user(4L, "host");
        Groups group = group();
        group.updateStatus(GroupStatus.COMPLETED);
        MatchedMember guest = matchedMember(30L, group, guestUser, RoleStatus.GUEST);
        MatchedMember host = matchedMember(40L, group, hostUser, RoleStatus.HOST);
        guest.completeReading(Instant.now());
        host.completeReading(Instant.now());

        when(matchedMemberRepository.findAllTrackerMembersByGroupId(group.getId()))
                .thenReturn(List.of(guest, host));

        assertThatThrownBy(() -> trackerService.getTrackerDetail(group.getId(), guestUser))
                .isInstanceOf(GroupException.class)
                .extracting("code")
                .isEqualTo(GroupErrorCode.GROUP_TERMINATED);
        assertThatThrownBy(() -> trackerService.getTrackerDetail(group.getId(), hostUser))
                .isInstanceOf(GroupException.class)
                .extracting("code")
                .isEqualTo(GroupErrorCode.GROUP_TERMINATED);
    }

    @Test
    void trackerDetailAndListUseSameDDayForReadingStatus() {
        User guest = user(1L, "guest");
        User host = user(2L, "host");
        Groups group = group();

        MatchedMember me = matchedMember(10L, group, guest, RoleStatus.GUEST);
        MatchedMember partner = matchedMember(20L, group, host, RoleStatus.HOST);
        MemberBook myBook = memberBook(100L, group, me, true, "나의 책");
        MemberBook myPartnerBook = memberBook(101L, group, me, false, "내가 읽을 상대 책");
        MemberBook partnerBook = memberBook(200L, group, partner, true, "상대 책");
        MemberBook partnerPartnerBook = memberBook(201L, group, partner, false, "상대가 읽을 책");
        me.getMemberBooks().addAll(List.of(myBook, myPartnerBook));
        partner.getMemberBooks().addAll(List.of(partnerBook, partnerPartnerBook));
        ReflectionTestUtils.setField(me, "currentMemberBook", myBook);
        ReflectionTestUtils.setField(partner, "currentMemberBook", partnerBook);
        group.getMatchedMember().addAll(List.of(me, partner));

        when(matchedMemberRepository.findAllTrackerItemsByMemberId(
                guest.getId(),
                GroupStatus.COMPLETED,
                ReadingStatus.COMPLETED
        )).thenReturn(List.of(me));
        when(matchedMemberRepository.findAllTrackerMembersByGroupId(group.getId())).thenReturn(List.of(me, partner));
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(me.getId(), myBook.getId()))
                .thenReturn(false);

        TrackerListResDTO listResponse = trackerService.getTrackerList(guest);
        TrackerDetailResDTO detailResponse = trackerService.getTrackerDetail(group.getId(), guest);

        assertThat(listResponse.items().get(0).getRemainingDays()).isEqualTo(6);
        assertThat(detailResponse.dDay()).isEqualTo(6);
        assertThat(listResponse.items().get(0).getRemainingDays()).isEqualTo(detailResponse.dDay());
    }

    @Test
    void trackerDetailReturnsDisplayBookTitleAndStatusLabelSeparately() {
        User guest = user(1L, "guest");
        User host = user(2L, "host");
        Groups group = group();

        String longTitle = "아무도 미워하지 않고 한 계절이 지나갔다";
        String longPartnerTitle = "아몬드 (양장 특별 한정판) - 제10회 창비 청소년문학상 수상작";
        MatchedMember me = matchedMember(10L, group, guest, RoleStatus.GUEST);
        MatchedMember partner = matchedMember(20L, group, host, RoleStatus.HOST);
        MemberBook myBook = memberBook(100L, group, me, true, longTitle);
        MemberBook myPartnerBook = memberBook(101L, group, me, false, longPartnerTitle);
        MemberBook partnerBook = memberBook(200L, group, partner, true, longPartnerTitle);
        me.getMemberBooks().addAll(List.of(myBook, myPartnerBook));
        partner.getMemberBooks().add(partnerBook);
        ReflectionTestUtils.setField(me, "currentMemberBook", myBook);
        ReflectionTestUtils.setField(partner, "currentMemberBook", partnerBook);

        when(matchedMemberRepository.findAllTrackerMembersByGroupId(group.getId())).thenReturn(List.of(me, partner));
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(me.getId(), myBook.getId()))
                .thenReturn(false);

        TrackerDetailResDTO response = trackerService.getTrackerDetail(group.getId(), guest);

        assertThat(response.displayStatus()).isEqualTo(TrackerDisplayStatus.READING);
        assertThat(response.displayBookTitle()).isEqualTo(longTitle);
        assertThat(response.displayStatusLabel()).isEqualTo("읽는 중");
        assertThat(response.myBook().title()).isEqualTo(longTitle);
        assertThat(response.partnerBook().title()).isEqualTo(longPartnerTitle);
        assertThat(step(response, TrackerStepStatus.MY_BOOK_READING).title())
                .isEqualTo("아무도 미워하지 않고 한 계절... 읽기");
        assertThat(step(response, TrackerStepStatus.MY_BOOK_REVIEWING).description())
                .isEqualTo("아무도 미워하지 않고 한 계절...의 책 후기를 작성해주세요");
        assertThat(step(response, TrackerStepStatus.PARTNER_BOOK_READING).title())
                .isEqualTo("아몬드 (양장 특별 한정판) ... 읽기");
        assertThat(step(response, TrackerStepStatus.PARTNER_BOOK_REVIEWING).description())
                .isEqualTo("아몬드 (양장 특별 한정판) ...의 책 후기를 작성해주세요");
    }

    @Test
    void trackerDetailKeepsSixteenCodePointBookTitleInStepDescription() {
        User guest = user(1L, "guest");
        User host = user(2L, "host");
        Groups group = group();

        String title = "1234567890123456";
        MatchedMember me = matchedMember(10L, group, guest, RoleStatus.GUEST);
        MatchedMember partner = matchedMember(20L, group, host, RoleStatus.HOST);
        MemberBook myBook = memberBook(100L, group, me, true, title);
        MemberBook myPartnerBook = memberBook(101L, group, me, false, "상대 책");
        MemberBook partnerBook = memberBook(200L, group, partner, true, "상대 책");
        me.getMemberBooks().addAll(List.of(myBook, myPartnerBook));
        partner.getMemberBooks().add(partnerBook);
        ReflectionTestUtils.setField(me, "currentMemberBook", myBook);
        ReflectionTestUtils.setField(partner, "currentMemberBook", partnerBook);

        when(matchedMemberRepository.findAllTrackerMembersByGroupId(group.getId())).thenReturn(List.of(me, partner));
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(me.getId(), myBook.getId()))
                .thenReturn(false);

        TrackerDetailResDTO response = trackerService.getTrackerDetail(group.getId(), guest);

        assertThat(response.displayBookTitle()).isEqualTo(title);
        assertThat(response.displayStatusLabel()).isEqualTo("읽는 중");
        assertThat(response.myBook().title()).isEqualTo(title);
        assertThat(step(response, TrackerStepStatus.MY_BOOK_READING).title())
                .isEqualTo("1234567890123456 읽기");
        assertThat(step(response, TrackerStepStatus.MY_BOOK_REVIEWING).description())
                .isEqualTo("1234567890123456의 책 후기를 작성해주세요");
    }

    @Test
    void trackerDetailKeepsSeventeenCodePointBookTitleInTopStatusText() {
        User guest = user(1L, "guest");
        User host = user(2L, "host");
        Groups group = group();

        String title = "12345678901234567";
        MatchedMember me = matchedMember(10L, group, guest, RoleStatus.GUEST);
        MatchedMember partner = matchedMember(20L, group, host, RoleStatus.HOST);
        MemberBook myBook = memberBook(100L, group, me, true, title);
        MemberBook myPartnerBook = memberBook(101L, group, me, false, "상대 책");
        MemberBook partnerBook = memberBook(200L, group, partner, true, "상대 책");
        me.getMemberBooks().addAll(List.of(myBook, myPartnerBook));
        partner.getMemberBooks().add(partnerBook);
        ReflectionTestUtils.setField(me, "currentMemberBook", myBook);
        ReflectionTestUtils.setField(partner, "currentMemberBook", partnerBook);

        when(matchedMemberRepository.findAllTrackerMembersByGroupId(group.getId())).thenReturn(List.of(me, partner));
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(me.getId(), myBook.getId()))
                .thenReturn(false);

        TrackerDetailResDTO response = trackerService.getTrackerDetail(group.getId(), guest);

        assertThat(response.displayBookTitle()).isEqualTo(title);
        assertThat(response.displayStatusLabel()).isEqualTo("읽는 중");
        assertThat(response.myBook().title()).isEqualTo(title);
        assertThat(step(response, TrackerStepStatus.MY_BOOK_READING).title())
                .isEqualTo("1234567890123456... 읽기");
        assertThat(step(response, TrackerStepStatus.MY_BOOK_REVIEWING).description())
                .isEqualTo("1234567890123456...의 책 후기를 작성해주세요");
    }

    @Test
    void directHostReceivesMeetingRegistrationCtaBeforeMeetingExists() {
        TrackerDetailResDTO response = directMeetingDetail(RoleStatus.HOST);

        assertThat(response.displayStatus()).isEqualTo(TrackerDisplayStatus.MEETING_REGISTER_REQUIRED);
    }

    @Test
    void directGuestReceivesWaitingStatusAndCommentCtaBeforeMeetingExists() {
        TrackerDetailResDTO response = directMeetingDetail(RoleStatus.GUEST);

        assertThat(response.displayStatus()).isEqualTo(TrackerDisplayStatus.WAITING_HOST_MEETING_REGISTER);
    }

    @Test
    void packageTrackerDetailKeepsBookDisplayWhenOnlyMeRegisteredTracking() {
        TrackerDetailResDTO response = deliveryExchangeDetail(
                ReadingStatus.EXCHANGING,
                ExchangeStatus.TRACKING_REGISTERED,
                ExchangeStatus.TRACKING_REGISTER_WAITING,
                false
        );

        assertThat(response.myBook().title()).isEqualTo("내 책");
        assertThat(response.partnerBook().title()).isEqualTo("상대 책");
        assertThat(response.displayBookTitle()).isEqualTo("내 책");
        assertMyOriginalBookFlags(response, true, false);
    }

    @Test
    void packageTrackerDetailSwapsBookDisplayWhenBothMembersRegisteredTracking() {
        TrackerDetailResDTO response = deliveryExchangeDetail(
                ReadingStatus.EXCHANGING,
                ExchangeStatus.TRACKING_REGISTERED,
                ExchangeStatus.TRACKING_REGISTERED,
                false
        );

        assertThat(response.myBook().title()).isEqualTo("상대 책");
        assertThat(response.partnerBook().title()).isEqualTo("내 책");
        assertThat(response.displayBookTitle()).isEqualTo("상대 책");
        assertMyOriginalBookFlags(response, false, true);
    }

    @Test
    void packageTrackerDetailKeepsSwappedBookDisplayWhenMineReceivedAndPartnerRegistered() {
        TrackerDetailResDTO response = deliveryExchangeDetail(
                ReadingStatus.EXCHANGING,
                ExchangeStatus.RECEIVED_CONFIRMED,
                ExchangeStatus.TRACKING_REGISTERED,
                false
        );

        assertThat(response.myBook().title()).isEqualTo("상대 책");
        assertThat(response.partnerBook().title()).isEqualTo("내 책");
        assertThat(response.displayBookTitle()).isEqualTo("상대 책");
        assertMyOriginalBookFlags(response, false, true);
    }

    @Test
    void packageReturningTrackerDetailMarksOnlyMyOriginalBookAfterDisplaySwap() {
        TrackerDetailResDTO response = deliveryExchangeDetail(
                ReadingStatus.RETURNING,
                ExchangeStatus.TRACKING_REGISTERED,
                ExchangeStatus.TRACKING_REGISTERED,
                true
        );

        assertThat(response.myBook().title()).isEqualTo("내 책");
        assertThat(response.partnerBook().title()).isEqualTo("상대 책");
        assertThat(response.displayBookTitle()).isEqualTo("내 책");
        assertMyOriginalBookFlags(response, true, false);
    }

    @Test
    void packageTrackerDetailKeepsExistingBookDisplayAfterFirstExchangeCompletes() {
        TrackerDetailResDTO response = deliveryExchangeDetail(
                ReadingStatus.PARTNER_BOOK_READING,
                ExchangeStatus.NOT_STARTED,
                ExchangeStatus.NOT_STARTED,
                true
        );

        assertThat(response.myBook().title()).isEqualTo("상대 책");
        assertThat(response.partnerBook().title()).isEqualTo("내 책");
        assertThat(response.displayBookTitle()).isEqualTo("상대 책");
        assertMyOriginalBookFlags(response, false, true);
    }

    @Test
    void directTrackerDetailDoesNotSwapBookDisplayBeforeMeetingCompletionChangesCurrentBooks() {
        TrackerDetailResDTO response = directMeetingDetail(RoleStatus.HOST);

        assertThat(response.myBook().title()).isEqualTo("내 책");
        assertThat(response.partnerBook().title()).isEqualTo("상대 책");
        assertThat(response.displayBookTitle()).isEqualTo("내 책");
        assertMyOriginalBookFlags(response, true, false);
    }

    private TrackerDetailResDTO directMeetingDetail(RoleStatus myRole) {
        User meUser = user(11L, "me");
        User partnerUser = user(12L, "partner");
        Groups group = Groups.builder()
                .id(2L)
                .groupName("직접 교환")
                .tradeType(TradeType.DIRECT)
                .startDate(TODAY)
                .readingPeriod(7)
                .matchedMember(new ArrayList<>())
                .build();
        RoleStatus partnerRole = myRole == RoleStatus.HOST ? RoleStatus.GUEST : RoleStatus.HOST;
        MatchedMember me = matchedMember(21L, group, meUser, myRole);
        MatchedMember partner = matchedMember(22L, group, partnerUser, partnerRole);
        me.updateReadingStatus(ReadingStatus.EXCHANGING);
        partner.updateReadingStatus(ReadingStatus.EXCHANGING);
        me.updateExchangeStatus(ExchangeStatus.MEETING_SCHEDULE_WAITING);
        partner.updateExchangeStatus(ExchangeStatus.MEETING_SCHEDULE_WAITING);
        MemberBook myBook = memberBook(301L, group, me, true, "내 책");
        MemberBook myPartnerBook = memberBook(303L, group, me, false, "상대 책");
        MemberBook partnerBook = memberBook(302L, group, partner, true, "상대 책");
        MemberBook partnerPartnerBook = memberBook(304L, group, partner, false, "내 책");
        me.getMemberBooks().addAll(List.of(myBook, myPartnerBook));
        partner.getMemberBooks().addAll(List.of(partnerBook, partnerPartnerBook));
        ReflectionTestUtils.setField(me, "currentMemberBook", myBook);
        ReflectionTestUtils.setField(partner, "currentMemberBook", partnerBook);

        when(matchedMemberRepository.findAllTrackerMembersByGroupId(group.getId()))
                .thenReturn(List.of(me, partner));
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(me.getId(), myBook.getId()))
                .thenReturn(false);

        return trackerService.getTrackerDetail(group.getId(), meUser);
    }

    private TrackerDetailResDTO deliveryExchangeDetail(
            ReadingStatus readingStatus,
            ExchangeStatus myExchangeStatus,
            ExchangeStatus partnerExchangeStatus,
            boolean currentBooksAlreadySwapped
    ) {
        User meUser = user(31L, "me");
        User partnerUser = user(32L, "partner");
        Groups group = group();
        MatchedMember me = matchedMember(41L, group, meUser, RoleStatus.GUEST);
        MatchedMember partner = matchedMember(42L, group, partnerUser, RoleStatus.HOST);
        me.updateReadingStatus(readingStatus);
        partner.updateReadingStatus(readingStatus);
        me.updateExchangeStatus(myExchangeStatus);
        partner.updateExchangeStatus(partnerExchangeStatus);

        MemberBook myBook = memberBook(501L, group, me, true, "내 책");
        MemberBook myPartnerBook = memberBook(503L, group, me, false, "상대 책");
        MemberBook partnerBook = memberBook(502L, group, partner, true, "상대 책");
        MemberBook partnerPartnerBook = memberBook(504L, group, partner, false, "내 책");
        me.getMemberBooks().addAll(List.of(myBook, myPartnerBook));
        partner.getMemberBooks().addAll(List.of(partnerBook, partnerPartnerBook));

        ReflectionTestUtils.setField(
                me,
                "currentMemberBook",
                currentBooksAlreadySwapped ? myPartnerBook : myBook
        );
        ReflectionTestUtils.setField(
                partner,
                "currentMemberBook",
                currentBooksAlreadySwapped ? partnerPartnerBook : partnerBook
        );

        when(matchedMemberRepository.findAllTrackerMembersByGroupId(group.getId()))
                .thenReturn(List.of(me, partner));
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(
                me.getId(),
                me.getCurrentMemberBook().getId()
        )).thenReturn(false);

        return trackerService.getTrackerDetail(group.getId(), meUser);
    }

    private User user(Long id, String nickname) {
        return User.builder()
                .id(id)
                .nickName(nickname)
                .socialType(SocialType.KAKAO)
                .socialId("social-" + id)
                .build();
    }

    private Groups group() {
        return Groups.builder()
                .id(1L)
                .groupName("교환 독서")
                .tradeType(TradeType.DELIVERY)
                .startDate(TODAY)
                .readingPeriod(7)
                .matchedMember(new ArrayList<>())
                .build();
    }

    private MatchedMember matchedMember(Long id, Groups group, User user, RoleStatus role) {
        return MatchedMember.builder()
                .id(id)
                .group(group)
                .user(user)
                .role(role)
                .readingStatus(ReadingStatus.MY_BOOK_READING)
                .build();
    }

    private MemberBook memberBook(Long id, Groups group, MatchedMember matchedMember, boolean isMine, String title) {
        return MemberBook.builder()
                .id(id)
                .group(group)
                .matchedMember(matchedMember)
                .book(book(id, title))
                .isMine(isMine)
                .currentPage(10)
                .build();
    }

    private Book book(Long id, String title) {
        return Book.builder()
                .id(id)
                .title(title)
                .image("https://example.com/book-" + id + ".jpg")
                .totalPages(100)
                .build();
    }

    private TrackerStepInfo step(TrackerDetailResDTO response, TrackerStepStatus status) {
        return response.steps().stream()
                .filter(step -> step.status() == status)
                .findFirst()
                .orElseThrow();
    }

    private void assertMyOriginalBookFlags(
            TrackerDetailResDTO response,
            boolean expectedMyBook,
            boolean expectedPartnerBook
    ) {
        assertThat(response.myBook().isMyOriginalBook()).isEqualTo(expectedMyBook);
        assertThat(response.partnerBook().isMyOriginalBook()).isEqualTo(expectedPartnerBook);
    }
}
