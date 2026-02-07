package com.example.bookiibookii.domain.review.service;

import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.review.dto.req.BookReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.req.GroupReviewRequestDTO;
import com.example.bookiibookii.domain.review.entity.GroupReview;
import com.example.bookiibookii.domain.review.exception.ReviewException;
import com.example.bookiibookii.domain.review.exception.code.ReviewErrorCode;
import com.example.bookiibookii.domain.review.repository.GroupReviewRepository;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.enums.TrackerStatus;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserBadge;
import com.example.bookiibookii.domain.user.enums.Badge;
import com.example.bookiibookii.domain.user.repository.UserBadgeRepository;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
import com.example.bookiibookii.domain.userbook.repository.UserBookRepository;
import lombok.RequiredArgsConstructor;
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

    private final UserBookRepository userBookRepository;
    private final TrackerRepository trackerRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final GroupReviewRepository groupReviewRepository;
    private final UserBadgeRepository userBadgeRepository;

    @Transactional
    public void createBookReview(Long userBookId, BookReviewRequestDTO request, User user) {
        validateRating(request.rating());
        validateCommentLength(request.comment(), BOOK_COMMENT_MAX_LENGTH);

        UserBook userBook = userBookRepository.findById(userBookId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.USER_BOOK_NOT_FOUND));

        if (!userBook.getUser().getId().equals(user.getId())) {
            throw new ReviewException(ReviewErrorCode.NOT_USER_BOOK_OWNER);
        }

        ensureTrackerReturned(userBook.getGroup().getGroupId());

        userBook.updateReview(request.rating(), request.comment());
    }

    @Transactional
    public void createGroupReview(Long groupId, GroupReviewRequestDTO.CreateGroupReviewDTO request, User user) {
        validateRating(request.rating());
        validateCommentLength(request.comment(), GROUP_COMMENT_MAX_LENGTH);

        MatchedMember reviewer = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, user.getId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.MATCHED_MEMBER_NOT_FOUND));

        if (groupReviewRepository.existsByGroupIdAndReviewerUserId(groupId, user.getId())) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Long partnerUserId = matchedMemberRepository.findPartnerUserId(groupId, user.getId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.PARTNER_NOT_FOUND));

        MatchedMember reviewed = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, partnerUserId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.PARTNER_NOT_FOUND));

        ensureTrackerReturned(groupId);

        GroupReview groupReview = GroupReview.builder()
                .reviewer(reviewer)
                .reviewed(reviewed)
                .rating(request.rating())
                .comment(request.comment())
                .build();

        List<Badge> badgeCodes = request.badgeCodes();
        int badgeCount = 0;

        if (badgeCodes != null && !badgeCodes.isEmpty()) {
            badgeCount = badgeCodes.size();
            for (Badge badge : badgeCodes) {
                groupReview.addBadge(badge);
                increaseUserBadgeCount(reviewed.getUser(), badge);
            }
        }

        User targetUser = reviewed.getUser();
        targetUser.updateManner(request.rating(), badgeCount);

        groupReviewRepository.save(groupReview);
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

    private void ensureTrackerReturned(Long groupId) {
        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.TRACKER_NOT_FOUND));
        if (tracker.getTrackerStatus() != TrackerStatus.RETURNED) {
            throw new ReviewException(ReviewErrorCode.TRACKER_NOT_RETURNED);
        }
    }

    private void increaseUserBadgeCount(User user, Badge badge) {
        UserBadge userBadge = userBadgeRepository.findByUserAndBadge(user, badge)
                .orElseGet(() -> userBadgeRepository.save(UserBadge.builder()
                        .user(user)
                        .badge(badge)
                        .count(0)
                        .build()));
        userBadge.increaseCount();
        userBadgeRepository.save(userBadge);
    }


}
