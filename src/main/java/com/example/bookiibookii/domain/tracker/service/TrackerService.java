package com.example.bookiibookii.domain.tracker.service;

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
import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.tracker.converter.TrackerConverter;
import com.example.bookiibookii.domain.tracker.dto.req.ExtendReadingPeriodReqDTO;
import com.example.bookiibookii.domain.tracker.dto.req.ReadingProgressRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.*;
import com.example.bookiibookii.domain.tracker.dto.res.ExtendReadingPeriodResDTO;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
import com.example.bookiibookii.domain.tracker.resolver.*;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TrackerService {

    private final MatchedMemberRepository matchedMemberRepository;
    private final GroupsRepository groupsRepository;
    private final TrackerPartnerResolver trackerPartnerResolver;
    private final TrackerDisplayStatusResolver trackerDisplayStatusResolver;
    private final TrackerDueDateResolver trackerDueDateResolver;
    private final UserProfileImageUrlResolver userProfileImageUrlResolver;
    private final TrackerStepAssembler trackerStepAssembler;
    private final TrackerTopBannerResolver trackerTopBannerResolver;
    private final MeetingRepository meetingRepository;
    private final BookReviewRepository bookReviewRepository;
    private final DomainEventPublisher eventPublisher;

    // 트래커 리스트 조회
    public TrackerListResDTO getTrackerList(User user) {
        List<TrackerMemberPair> memberPairs = matchedMemberRepository.findAllTrackerItemsByMemberId(
                user.getId(),
                GroupStatus.COMPLETED,
                ReadingStatus.COMPLETED
        ).stream()
                .map(me -> new TrackerMemberPair(me, trackerPartnerResolver.resolve(me.getGroup(), me)))
                .toList();

        Map<MeetingKey, Meeting> meetingsByKey = loadMeetingsByKey(memberPairs);
        List<TrackerListItemResDTO> items = memberPairs.stream()
                .map(pair -> toTrackerListItem(pair.me(), pair.partner()))
                .toList();
        List<TrackerTopBannerContext> bannerContexts = memberPairs.stream()
                .map(pair -> toTopBannerContext(pair.me(), pair.partner(), meetingsByKey))
                .toList();

        return TrackerListResDTO.builder()
                .nickname(user.getNickName())
                .summary(buildListSummary(items))
                .topBanners(trackerTopBannerResolver.resolve(bannerContexts))
                .items(items)
                .build();
    }

    // 교환독서 상세조회
    @Transactional(readOnly = true)
    public TrackerDetailResDTO getTrackerDetail(Long groupId, User user) {
        List<MatchedMember> matchedMembers = matchedMemberRepository.findAllTrackerMembersByGroupId(groupId);

        MatchedMember me = matchedMembers.stream()
                .filter(matchedMember -> matchedMember.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND));

        MatchedMember partner = matchedMembers.stream()
                .filter(matchedMember -> !matchedMember.getUser().getId().equals(user.getId()))
                .findFirst()
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.PARTNER_NOT_FOUND));

        if (me.getCurrentMemberBook() == null || partner.getCurrentMemberBook() == null) {
            throw new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND);
        }

        boolean currentBookReviewWritten = isCurrentBookReviewWritten(me);
        TrackerDisplayStatus displayStatus = trackerDisplayStatusResolver.resolve(
                me,
                partner,
                currentBookReviewWritten
        );

        return TrackerConverter.toDetail(
                me,
                partner,
                displayStatus,
                trackerDueDateResolver.calculate(displayStatus, me.getGroup()),
                userProfileImageUrlResolver.resolve(me.getCurrentMemberBook().getMatchedMember().getUser()),
                userProfileImageUrlResolver.resolve(partner.getCurrentMemberBook().getMatchedMember().getUser()),
                trackerStepAssembler.assemble(me, currentBookReviewWritten)
        );
    }

    @Transactional
    public ReadingProgressResponseDTO updateReadingProgress(Long groupId, ReadingProgressRequestDTO request, User user) {
        if (request == null || request.currentPage() == null || request.currentPage() < 0) {
            throw new TrackerException(TrackerErrorCode.INVALID_READING_PROGRESS);
        }

        MatchedMember me = getMyMatchedMember(groupId, user.getId());
        if (me.getCurrentMemberBook() == null || me.getCurrentMemberBook().getBook() == null) {
            throw new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND);
        }

        Integer totalPages = me.getCurrentMemberBook().getBook().getTotalPages();
        if (totalPages == null || totalPages <= 0) {
            throw new TrackerException(TrackerErrorCode.INVALID_BOOK_TOTAL_PAGES);
        }

        if (request.currentPage() > totalPages) {
            throw new TrackerException(TrackerErrorCode.INVALID_READING_PROGRESS);
        }

        int normalizedPage = request.currentPage();
        ReadingStatus currentStatus = me.getReadingStatus();
        validateReadingProgressStatus(currentStatus, normalizedPage, totalPages);

        me.getCurrentMemberBook().updateCurrentPage(normalizedPage);

        if (normalizedPage == totalPages) {
            if (currentStatus == ReadingStatus.MY_BOOK_READING) {
                me.updateReadingStatus(ReadingStatus.MY_BOOK_REVIEWING);
            } else if (currentStatus == ReadingStatus.PARTNER_BOOK_READING) {
                me.updateReadingStatus(ReadingStatus.PARTNER_BOOK_REVIEWING);
            }
            publishRoundCompletionIfReviewExists(me, currentStatus, user);
        }

        ReadingStatus updatedStatus = me.getReadingStatus();
        return ReadingProgressResponseDTO.builder()
                .memberBookId(me.getCurrentMemberBook().getId())
                .currentPage(me.getCurrentMemberBook().getCurrentPage())
                .totalPages(totalPages)
                .progressRate(calculateProgressRate(me.getCurrentMemberBook().getCurrentPage(), totalPages))
                .readingStatus(updatedStatus)
                .readingStatusText(updatedStatus.getDescription())
                .dDay(calculateReadingDDay(updatedStatus, me.getGroup()))
                .build();
    }

    @Transactional
    public ExtendReadingPeriodResDTO extendReadingPeriod(Long groupId, ExtendReadingPeriodReqDTO request, User user) {
        RoleStatus role = matchedMemberRepository.findRoleByGroupIdAndUserId(groupId, user.getId())
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));
        if (role != RoleStatus.HOST) {
            throw new TrackerException(TrackerErrorCode.NOT_GROUP_HOST);
        }

        Groups group = groupsRepository.findById(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        int newPeriod = ReadingPeriodDateCalculator.inclusivePeriod(group.getStartDate(), request.newEndDate());
        if (Objects.equals(newPeriod, group.getReadingPeriod())) {
            int currentDDay = ReadingPeriodDateCalculator.remainingDaysUntil(
                    request.newEndDate(),
                    ReadingPeriodDateCalculator.todayKst()
            );
            return new ExtendReadingPeriodResDTO(request.newEndDate(), currentDDay);
        }
        group.setReadingPeriod(newPeriod);
        matchedMemberRepository.findByGroup_IdAndRole(groupId, RoleStatus.GUEST)
                .map(MatchedMember::getUser)
                .filter(receiver -> !receiver.getId().equals(user.getId()))
                .ifPresent(receiver -> eventPublisher.publish(new TrackerNotificationEvent(
                        NotificationType.NOTI_TRK_002,
                        user.getId(),
                        null,
                        user.getNickName(),
                        List.of(receiver.getId()),
                        groupId,
                        ExchangeType.from(group.getTradeType()),
                        null,
                        null,
                        null,
                        null,
                        null,
                        request.newEndDate()
                )));

        int dDay = ReadingPeriodDateCalculator.remainingDaysUntil(
                request.newEndDate(),
                ReadingPeriodDateCalculator.todayKst()
        );
        return new ExtendReadingPeriodResDTO(request.newEndDate(), dDay);
    }

    private void validateReadingProgressStatus(ReadingStatus status, int normalizedPage, int totalPages) {
        if (status == ReadingStatus.MY_BOOK_READING || status == ReadingStatus.PARTNER_BOOK_READING) {
            return;
        }

        if ((status == ReadingStatus.MY_BOOK_REVIEWING || status == ReadingStatus.PARTNER_BOOK_REVIEWING)
                && normalizedPage == totalPages) {
            return;
        }

        throw new TrackerException(TrackerErrorCode.INVALID_TRACKER_STATUS);
    }

    private void publishRoundCompletionIfReviewExists(
            MatchedMember me,
            ReadingStatus previousStatus,
            User actor
    ) {
        if (previousStatus != ReadingStatus.MY_BOOK_READING
                && previousStatus != ReadingStatus.PARTNER_BOOK_READING) {
            return;
        }
        if (!bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(
                me.getId(),
                me.getCurrentMemberBook().getId()
        )) {
            return;
        }

        matchedMemberRepository.findPartnerUserId(me.getGroup().getId(), actor.getId())
                .filter(receiverId -> !receiverId.equals(actor.getId()))
                .ifPresent(receiverId -> eventPublisher.publish(new TrackerNotificationEvent(
                        NotificationType.NOTI_TRK_001,
                        actor.getId(),
                        me.getId(),
                        actor.getNickName(),
                        List.of(receiverId),
                        me.getGroup().getId(),
                        ExchangeType.from(me.getGroup().getTradeType()),
                        me.getCurrentMemberBook().getBook().getTitle(),
                        me.getCurrentMemberBook().getBook().getId(),
                        null,
                        null,
                        previousStatus == ReadingStatus.MY_BOOK_READING
                                ? ExchangeRound.FIRST_EXCHANGE
                                : ExchangeRound.RETURN_EXCHANGE,
                        null
                )));
    }

    private int calculateProgressRate(Integer currentPage, Integer totalPages) {
        if (currentPage == null || totalPages == null || totalPages <= 0) {
            return 0;
        }
        int normalizedPage = Math.min(Math.max(currentPage, 0), totalPages);
        return (normalizedPage * 100) / totalPages;
    }

    private Integer calculateReadingDDay(ReadingStatus readingStatus, Groups group) {
        if (readingStatus != ReadingStatus.MY_BOOK_READING
                && readingStatus != ReadingStatus.PARTNER_BOOK_READING) {
            return null;
        }
        return trackerDueDateResolver.calculate(TrackerDisplayStatus.READING, group);
    }

    private TrackerListResDTO.Summary buildListSummary(List<TrackerListItemResDTO> items) {
        int readingCount = 0;
        int exchangingCount = 0;
        int reviewCount = 0;

        for (TrackerListItemResDTO item : items) {
            TrackerDisplayStatus displayStatus = item.getDisplayStatus();
            if (displayStatus == TrackerDisplayStatus.READING) {
                readingCount++;
            } else if (isReviewStatus(displayStatus)) {
                reviewCount++;
            } else if (isExchangeStatus(displayStatus)) {
                exchangingCount++;
            }
        }

        return TrackerListResDTO.Summary.builder()
                .totalCount(items.size())
                .readingCount(readingCount)
                .exchangingCount(exchangingCount)
                .reviewCount(reviewCount)
                .build();
    }

    private boolean isReviewStatus(TrackerDisplayStatus displayStatus) {
        return displayStatus == TrackerDisplayStatus.REVIEW_WRITING
                || displayStatus == TrackerDisplayStatus.REVIEW_WAITING_PARTNER
                || displayStatus == TrackerDisplayStatus.EXCHANGE_REVIEW_WRITING;
    }

    private boolean isExchangeStatus(TrackerDisplayStatus displayStatus) {
        return displayStatus == TrackerDisplayStatus.TRACKING_REQUIRED
                || displayStatus == TrackerDisplayStatus.SHIPPING
                || displayStatus == TrackerDisplayStatus.RETURN_TRACKING_REQUIRED
                || displayStatus == TrackerDisplayStatus.RETURNING
                || displayStatus == TrackerDisplayStatus.MEETING_REQUIRED
                || displayStatus == TrackerDisplayStatus.WAITING_PARTNER_TRACKING_REGISTER
                || displayStatus == TrackerDisplayStatus.WAITING_PARTNER_RECEIPT_CONFIRM
                || displayStatus == TrackerDisplayStatus.MEETING_REGISTER_REQUIRED
                || displayStatus == TrackerDisplayStatus.WAITING_HOST_MEETING_REGISTER
                || displayStatus == TrackerDisplayStatus.WAITING_PARTNER_MEETING_COMPLETE
                || displayStatus == TrackerDisplayStatus.EXCHANGING;
    }

    private MatchedMember getMyMatchedMember(Long groupId, Long userId) {
        return matchedMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));
    }

    private boolean isCurrentBookReviewWritten(MatchedMember me) {
        if (me.getCurrentMemberBook() == null) {
            return false;
        }
        return bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(
                me.getId(),
                me.getCurrentMemberBook().getId()
        );
    }

    private TrackerListItemResDTO toTrackerListItem(MatchedMember me, MatchedMember partner) {
        validateCurrentBooks(me, partner);
        TrackerDisplayStatus displayStatus = trackerDisplayStatusResolver.resolve(
                me,
                partner,
                isCurrentBookReviewWritten(me)
        );

        return TrackerConverter.toListItem(
                me,
                partner,
                displayStatus,
                trackerDueDateResolver.calculate(displayStatus, me.getGroup()),
                userProfileImageUrlResolver.resolve(me.getCurrentMemberBook().getMatchedMember().getUser()),
                userProfileImageUrlResolver.resolve(partner.getCurrentMemberBook().getMatchedMember().getUser())
        );
    }

    private TrackerTopBannerContext toTopBannerContext(
            MatchedMember me,
            MatchedMember partner,
            Map<MeetingKey, Meeting> meetingsByKey
    ) {
        validateCurrentBooks(me, partner);
        ExchangeRound exchangeRound = resolveExchangeRound(me.getReadingStatus());
        Meeting meeting = exchangeRound == null
                ? null
                : meetingsByKey.get(new MeetingKey(me.getGroup().getId(), exchangeRound));
        String currentBookTitle = bookTitle(me.getCurrentMemberBook());

        return new TrackerTopBannerContext(
                me.getGroup().getId(),
                me.getId(),
                me.getGroup().getGroupName(),
                partner.getUser().getNickName(),
                me.getGroup().getTradeType(),
                me.getRole(),
                me.getReadingStatus(),
                me.getExchangeStatus(),
                partner.getExchangeStatus(),
                ReadingPeriodDateCalculator.endDate(me.getGroup()),
                currentBookTitle,
                exchangeRound,
                exchangeRound == ExchangeRound.FIRST_EXCHANGE ? currentBookTitle : null,
                exchangeRound == ExchangeRound.RETURN_EXCHANGE ? currentBookTitle : null,
                me.isReviewWritten(),
                meeting == null ? null : meeting.getScheduledAt(),
                meeting == null ? null : meeting.getPlaceName()
        );
    }

    private Map<MeetingKey, Meeting> loadMeetingsByKey(List<TrackerMemberPair> memberPairs) {
        List<Long> directExchangeGroupIds = memberPairs.stream()
                .map(TrackerMemberPair::me)
                .filter(me -> me.getGroup().getTradeType() == TradeType.DIRECT)
                .filter(me -> resolveExchangeRound(me.getReadingStatus()) != null)
                .map(me -> me.getGroup().getId())
                .distinct()
                .toList();
        if (directExchangeGroupIds.isEmpty()) {
            return Map.of();
        }

        return meetingRepository.findAllByGroupIds(directExchangeGroupIds).stream()
                .collect(Collectors.toMap(
                        meeting -> new MeetingKey(meeting.getGroup().getId(), meeting.getExchangeRound()),
                        Function.identity()
                ));
    }

    private ExchangeRound resolveExchangeRound(ReadingStatus readingStatus) {
        if (readingStatus == ReadingStatus.EXCHANGING) {
            return ExchangeRound.FIRST_EXCHANGE;
        }
        if (readingStatus == ReadingStatus.RETURNING) {
            return ExchangeRound.RETURN_EXCHANGE;
        }
        return null;
    }

    private String bookTitle(MemberBook memberBook) {
        return memberBook.getBook().getTitle();
    }

    private void validateCurrentBooks(MatchedMember me, MatchedMember partner) {
        if (me.getCurrentMemberBook() == null || partner.getCurrentMemberBook() == null) {
            throw new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND);
        }
    }

    private record TrackerMemberPair(MatchedMember me, MatchedMember partner) {
    }

    private record MeetingKey(Long groupId, ExchangeRound exchangeRound) {
    }
}
