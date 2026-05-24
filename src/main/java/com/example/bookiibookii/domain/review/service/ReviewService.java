package com.example.bookiibookii.domain.review.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.groupbook.entity.GroupBook;
import com.example.bookiibookii.domain.review.dto.req.ReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.res.BookReviewResponseDTO;
import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.review.entity.GroupReview;
import com.example.bookiibookii.domain.review.exception.ReviewException;
import com.example.bookiibookii.domain.review.exception.code.ReviewErrorCode;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.review.repository.GroupReviewRepository;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import com.example.bookiibookii.domain.tracker.service.DeliveryAddressService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.groupbook.repository.GroupBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final double RATING_MIN = 0.0;
    private static final double RATING_MAX = 5.0;
    private static final int BOOK_COMMENT_MAX_LENGTH = 500;
    private static final int GROUP_COMMENT_MAX_LENGTH = 200;

    private final GroupBookRepository groupBookRepository;
    private final TrackerRepository trackerRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final BookReviewRepository bookReviewRepository;
    private final GroupReviewRepository groupReviewRepository;
    private final GroupsRepository groupsRepository;
    private final DeliveryAddressService deliveryAddressService;

    @Transactional
    public BookReviewResponseDTO createBookReview(
            Long groupId,
            ReviewRequestDTO.BookReviewUpsertDTO request,
            User user
    ) {
        validateRating(request.star());
        validateCommentLength(request.comment(), BOOK_COMMENT_MAX_LENGTH);

        Groups group = groupsRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        MatchedMember me = getMatchedMember(group.getId(), user.getId());
        MemberBook currentMemberBook = getCurrentMemberBook(me);

        if (bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(me.getId(), currentMemberBook.getId())) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        if (bookReviewRepository.existsByMemberBookId(currentMemberBook.getId())) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        BookReview bookReview = BookReview.create(
                me,
                currentMemberBook,
                request.star(),
                request.comment()
        );

        try {
            bookReview = bookReviewRepository.save(bookReview);
        } catch (DataIntegrityViolationException e) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // 교환상태 변경
        ReadingStatus currentStatus = me.getReadingStatus();
        if (currentStatus == ReadingStatus.MY_BOOK_REVIEWING) {
            me.updateReadingStatus(ReadingStatus.EXCHANGING);
        } else if (currentStatus == ReadingStatus.PARTNER_BOOK_REVIEWING) {
            me.updateReadingStatus(ReadingStatus.RETURNING);
        } else {
            throw new ReviewException(ReviewErrorCode.INVALID_REVIEW_READING_STATUS);
        }

        updateExchangeStatusIfAllMembersReady(group.getId(), group.getTradeType());

        return BookReviewResponseDTO.from(bookReview);
    }

    @Transactional
    public BookReviewResponseDTO updateMyBookReview(
            Long groupId,
            ReviewRequestDTO.BookReviewUpsertDTO request,
            User user
    ) {
        validateRating(request.star());
        validateCommentLength(request.comment(), BOOK_COMMENT_MAX_LENGTH);

        MatchedMember me = getMatchedMember(groupId, user.getId());
        MemberBook currentMemberBook = getCurrentMemberBook(me);

        BookReview bookReview = bookReviewRepository
                .findByMatchedMember_IdAndMemberBook_Id(me.getId(), currentMemberBook.getId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.BOOK_REVIEW_NOT_FOUND));

        bookReview.updateReview(request.star(), request.comment());

        return BookReviewResponseDTO.from(bookReview);
    }

    /**
     * [릴레이] 통합 리뷰 생성 (릴레이 종료 후)
     * 책 리뷰(GroupBook)와 상대방 리뷰(GroupReview)를 한 번에 저장하고 트래커를 종료합니다.
     */
    @Transactional
    public void createRelayReview(Long groupBookId, ReviewRequestDTO.RelayReviewDTO request, User user) {
        // 2-1. 모든 평점 및 코멘트 검증
        validateRating(request.bookRating());
        validateRating(request.partnerRating());
        validateCommentLength(request.bookComment(), BOOK_COMMENT_MAX_LENGTH);
        validateCommentLength(request.partnerComment(), GROUP_COMMENT_MAX_LENGTH);

        // 2-2. GroupBook 조회 및 권한 확인
        GroupBook groupBook = groupBookRepository.findById(groupBookId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.GROUP_BOOK_NOT_FOUND));
        if (!groupBook.getUser().getId().equals(user.getId())) {
            throw new ReviewException(ReviewErrorCode.NOT_GROUP_BOOK_OWNER);
        }

        // [보완] 비관적 락을 사용하여 그룹 조회 (동시성 제어)
        Groups group = groupsRepository.findByIdForUpdate(groupBook.getGroup().getId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        // [보완] 그룹 상태가 MATCHED일 때만 리뷰 프로세스 진행
        if (group.getGroupStatus() != GroupStatus.MATCHED) {
            // 이미 COMPLETED거나 DELETED인 경우 예외를 던지거나 리턴 처리
            throw new GroupException(GroupErrorCode.INVALID_GROUP_STATUS);
        }

        Long groupId = group.getId();
        ensureTrackerReturned(groupId);

        // 2-3. [책 리뷰] 업데이트
        groupBook.updateReview(request.bookRating(), request.bookComment());

        // 2-4. 리뷰어 조회 및 중복 체크
        MatchedMember reviewer = matchedMemberRepository.findByGroup_IdAndUser_Id(groupId, user.getId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.MATCHED_MEMBER_NOT_FOUND));

        if (groupReviewRepository.existsByGroupIdAndReviewerUserId(groupId, user.getId())) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // 2-5. 내 상태 업데이트
        reviewer.markReviewAsWritten(); // isReviewWritten = true

        // 2-6. 상대방 조회 및 리뷰 저장
        Long partnerUserId = matchedMemberRepository.findPartnerUserId(groupId, user.getId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.PARTNER_NOT_FOUND));

        MatchedMember reviewed = matchedMemberRepository.findByGroup_IdAndUser_Id(groupId, partnerUserId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.PARTNER_NOT_FOUND));

        GroupReview groupReview = GroupReview.builder()
                .reviewer(reviewer)
                .reviewed(reviewed)
                .rating(request.partnerRating())
                .comment(request.partnerComment())
                .build();

        groupReviewRepository.save(groupReview);

        // [핵심] 락이 걸린 상태에서 전원 완료 여부 체크
        long remainingCount = matchedMemberRepository.countByGroup_IdAndIsReviewWrittenFalse(groupId);

        if (remainingCount == 0) {
            group.updateStatus(GroupStatus.COMPLETED);
        }
    }

    private void validateRating(Double rating) {
        if (rating == null || rating < RATING_MIN || rating > RATING_MAX || (rating * 2) % 1 != 0) {
            throw new ReviewException(ReviewErrorCode.INVALID_RATING);
        }
    }

    private void validateCommentLength(String comment, int limit) {
        if (comment != null && comment.length() > limit) {
            throw new ReviewException(ReviewErrorCode.COMMENT_TOO_LONG);
        }
    }

    private Tracker ensureTrackerReturned(Long groupId) {
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.TRACKER_NOT_FOUND));
        if (tracker.getReadingStatus() != ReadingStatus.COMPLETED) {
            throw new ReviewException(ReviewErrorCode.TRACKER_NOT_RETURNED);
        }

        return tracker;
    }

    private MatchedMember getMatchedMember(Long groupId, Long userId) {
        return matchedMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.NOT_GROUP_MEMBER));
    }

    private MemberBook getCurrentMemberBook(MatchedMember matchedMember) {
        MemberBook currentMemberBook = matchedMember.getCurrentMemberBook();
        if (currentMemberBook == null) {
            throw new ReviewException(ReviewErrorCode.CURRENT_MEMBER_BOOK_NOT_FOUND);
        }
        return currentMemberBook;
    }

    private void updateExchangeStatusIfAllMembersReady(Long groupId, TradeType tradeType) {
        List<MatchedMember> matchedMembers = matchedMemberRepository.findAllByGroup_Id(groupId);
        if (matchedMembers.isEmpty()) {
            throw new ReviewException(ReviewErrorCode.MATCHED_MEMBER_NOT_FOUND);
        }
        if (matchedMembers.size() != 2) {
            return;
        }

        boolean allExchanging = matchedMembers.stream()
                .allMatch(member -> member.getReadingStatus() == ReadingStatus.EXCHANGING);
        boolean allReturning = matchedMembers.stream()
                .allMatch(member -> member.getReadingStatus() == ReadingStatus.RETURNING);

        if (!allExchanging && !allReturning) {
            return;
        }

        ExchangeStatus initialExchangeStatus = resolveInitialExchangeStatus(tradeType);
        matchedMembers.forEach(member -> member.updateExchangeStatus(initialExchangeStatus));

        if (tradeType == TradeType.DELIVERY && allExchanging) {
            deliveryAddressService.createFirstExchangeAddressesIfAbsent(groupId);
        }
        if (tradeType == TradeType.DELIVERY && allReturning) {
            deliveryAddressService.createReturnExchangeAddressesIfAbsent(groupId);
        }
    }

    private ExchangeStatus resolveInitialExchangeStatus(TradeType tradeType) {
        if (tradeType == TradeType.DIRECT) {
            return ExchangeStatus.MEETING_SCHEDULE_WAITING;
        }
        return ExchangeStatus.TRACKING_REGISTER_WAITING;
    }
}
