package com.example.bookiibookii.domain.review.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.review.dto.req.ReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.res.BookReviewResponseDTO;
import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.review.exception.ReviewException;
import com.example.bookiibookii.domain.review.exception.code.ReviewErrorCode;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.service.DeliveryAddressService;
import com.example.bookiibookii.domain.user.entity.User;
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

    private final MatchedMemberRepository matchedMemberRepository;
    private final BookReviewRepository bookReviewRepository;
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
