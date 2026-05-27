package com.example.bookiibookii.domain.tracker.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.tracker.converter.TrackerConverter;
import com.example.bookiibookii.domain.tracker.dto.req.ExtendReadingPeriodReqDTO;
import com.example.bookiibookii.domain.tracker.dto.req.ReadingProgressRequestDTO;
import com.example.bookiibookii.domain.tracker.dto.res.*;
import com.example.bookiibookii.domain.tracker.dto.res.ExtendReadingPeriodResDTO;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import com.example.bookiibookii.domain.tracker.exception.TrackerException;
import com.example.bookiibookii.domain.tracker.exception.code.TrackerErrorCode;
import com.example.bookiibookii.domain.tracker.resolver.*;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

    // 트래커 리스트 조회
    public TrackerListResDTO getTrackerList(User user) {
        List<TrackerListItemResDTO> items = matchedMemberRepository.findAllTrackerItemsByMemberId(
                        user.getId(),
                        GroupStatus.COMPLETED,
                        ReadingStatus.COMPLETED
                ).stream()
                .map(me -> {
                    MatchedMember partner = trackerPartnerResolver.resolve(me.getGroup(), me);
                    if (me.getCurrentMemberBook() == null || partner.getCurrentMemberBook() == null) {
                        throw new TrackerException(TrackerErrorCode.TRACKER_NOT_FOUND);
                    }

                    TrackerDisplayStatus displayStatus = trackerDisplayStatusResolver.resolve(
                            me.getReadingStatus(),
                            me.getExchangeStatus(),
                            me.getGroup().getTradeType()
                    );

                    return TrackerConverter.toListItem(
                            me,
                            partner,
                            displayStatus,
                            trackerDueDateResolver.calculate(displayStatus, me.getGroup()),
                            userProfileImageUrlResolver.resolve(me.getCurrentMemberBook().getMatchedMember().getUser()),
                            userProfileImageUrlResolver.resolve(partner.getCurrentMemberBook().getMatchedMember().getUser())
                    );
                })
                .toList();

        return TrackerListResDTO.builder()
                .summary(buildListSummary(items))
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

        TrackerDisplayStatus displayStatus = trackerDisplayStatusResolver.resolve(
                me.getReadingStatus(),
                me.getExchangeStatus(),
                me.getGroup().getTradeType()
        );

        return TrackerConverter.toDetail(
                me,
                partner,
                displayStatus,
                trackerDueDateResolver.calculate(displayStatus, me.getGroup()),
                userProfileImageUrlResolver.resolve(me.getCurrentMemberBook().getMatchedMember().getUser()),
                userProfileImageUrlResolver.resolve(partner.getCurrentMemberBook().getMatchedMember().getUser()),
                trackerStepAssembler.assemble(me)
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
        }

        ReadingStatus updatedStatus = me.getReadingStatus();//
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

        long newPeriod = ChronoUnit.DAYS.between(group.getStartDate(), request.newEndDate()) + 1;
        group.setReadingPeriod((int) newPeriod);

        int dDay = (int) ChronoUnit.DAYS.between(LocalDate.now(), request.newEndDate()) + 1;
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
        if (group.getStartDate() == null || group.getReadingPeriod() == null) {
            return null;
        }

        LocalDate dueDate = group.getStartDate().plusDays(group.getReadingPeriod());
        return Math.max((int) ChronoUnit.DAYS.between(LocalDate.now(), dueDate), 0);
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
                || displayStatus == TrackerDisplayStatus.EXCHANGE_REVIEW_WRITING;
    }

    private boolean isExchangeStatus(TrackerDisplayStatus displayStatus) {
        return displayStatus == TrackerDisplayStatus.TRACKING_REQUIRED
                || displayStatus == TrackerDisplayStatus.SHIPPING
                || displayStatus == TrackerDisplayStatus.RETURN_TRACKING_REQUIRED
                || displayStatus == TrackerDisplayStatus.RETURNING
                || displayStatus == TrackerDisplayStatus.MEETING_REQUIRED
                || displayStatus == TrackerDisplayStatus.EXCHANGING;
    }

    private MatchedMember getMyMatchedMember(Long groupId, Long userId) {
        return matchedMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new TrackerException(TrackerErrorCode.NOT_GROUP_MEMBER));
    }
}
