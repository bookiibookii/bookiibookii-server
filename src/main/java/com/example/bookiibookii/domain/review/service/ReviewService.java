package com.example.bookiibookii.domain.review.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.review.dto.req.ReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.res.GroupReviewResponseDTO;
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

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final double RATING_MIN = 0.0;
    private static final double RATING_MAX = 5.0;
    private static final int BOOK_COMMENT_MAX_LENGTH = 500;
    private static final int GROUP_COMMENT_MAX_LENGTH = 200;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy. MM. dd.");

    private final UserBookRepository userBookRepository;
    private final TrackerRepository trackerRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final GroupReviewRepository groupReviewRepository;
    private final UserBadgeRepository userBadgeRepository;

    /**
     * 1. [함께 읽기] 리뷰 생성
     * 파트너가 없으므로 본인의 서재(UserBook)에 책 리뷰만 남깁니다.
     */
    @Transactional
    public void createTogetherReview(Long userBookId, ReviewRequestDTO.TogetherReviewDTO request, User user) {
        validateRating(request.rating());
        validateCommentLength(request.comment(), BOOK_COMMENT_MAX_LENGTH);

        UserBook userBook = userBookRepository.findById(userBookId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.USER_BOOK_NOT_FOUND));

        if (!userBook.getUser().getId().equals(user.getId())) {
            throw new ReviewException(ReviewErrorCode.NOT_USER_BOOK_OWNER);
        }

        userBook.updateReview(request.rating(), request.comment());
    }

    /**
     * 2. [릴레이] 통합 리뷰 생성
     * 책 리뷰(UserBook)와 상대방 리뷰(GroupReview)를 한 번에 저장하고 트래커를 종료합니다.
     */
    @Transactional
    public void createRelayReview(Long userBookId, ReviewRequestDTO.RelayReviewDTO request, User user) {
        // 2-1. 모든 평점 및 코멘트 검증
        validateRating(request.bookRating());
        validateRating(request.partnerRating());
        validateCommentLength(request.bookComment(), BOOK_COMMENT_MAX_LENGTH);
        validateCommentLength(request.partnerComment(), GROUP_COMMENT_MAX_LENGTH);

        // 2-2. UserBook 조회 및 권한 확인
        UserBook userBook = userBookRepository.findById(userBookId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.USER_BOOK_NOT_FOUND));
        if (!userBook.getUser().getId().equals(user.getId())) {
            throw new ReviewException(ReviewErrorCode.NOT_USER_BOOK_OWNER);
        }

        Groups group = userBook.getGroup();
        if (group == null) {
            throw new GroupException(GroupErrorCode.GROUP_NOT_FOUND);
        }
        Long groupId = group.getGroupId();

        // 2-3. 트래커 상태 확인 (RETURNED여야 최종 완료 가능)
        ensureTrackerReturned(groupId);

        // 2-4. [책 리뷰] 업데이트
        userBook.updateReview(request.bookRating(), request.bookComment());

        // 2-5. [상대방 리뷰] 생성 및 뱃지/매너점수 처리
        MatchedMember reviewer = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, user.getId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.MATCHED_MEMBER_NOT_FOUND));

        if (groupReviewRepository.existsByGroupIdAndReviewerUserId(groupId, user.getId())) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        Long partnerUserId = matchedMemberRepository.findPartnerUserId(groupId, user.getId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.PARTNER_NOT_FOUND));

        MatchedMember reviewed = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, partnerUserId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.PARTNER_NOT_FOUND));

        GroupReview groupReview = GroupReview.builder()
                .reviewer(reviewer)
                .reviewed(reviewed)
                .rating(request.partnerRating())
                .comment(request.partnerComment())
                .build();

        processGroupReview(reviewed.getUser(), groupReview, request.badgeCodes(), request.partnerRating());
        groupReviewRepository.save(groupReview);


    }

    /**
     * 그룹 리뷰(파트너 리뷰) 저장 시 뱃지 부여 및 매너 온도 업데이트 처리
     */
    private void processGroupReview(User targetUser, GroupReview groupReview, List<Badge> badgeCodes, Double rating) {
        int badgeCount = 0;
        if (badgeCodes != null && !badgeCodes.isEmpty()) {
            badgeCount = badgeCodes.size();
            for (Badge badge : badgeCodes) {
                groupReview.addBadge(badge);
                increaseUserBadgeCount(targetUser, badge);
            }
        }
        targetUser.updateManner(rating, badgeCount);
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
        if (tracker.getTrackerStatus() != TrackerStatus.RETURNED) {
            throw new ReviewException(ReviewErrorCode.TRACKER_NOT_RETURNED);
        }

        return tracker;
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


    @Transactional(readOnly = true)
    public GroupReviewResponseDTO.GroupReviewDetailDTO getMyRelayReviewHistory(User user) {
        // 1. 내가 받은 리뷰 목록 조회 (RELAY 그룹만)
        List<GroupReview> partnerToMeReviews = groupReviewRepository.findByReviewedUserIdAndGroupType(
                user.getId(), GroupType.RELAY);
        if (partnerToMeReviews == null || partnerToMeReviews.isEmpty()) {
            return GroupReviewResponseDTO.GroupReviewDetailDTO.builder().reviews(List.of()).build();
        }

        // 2. 등장하는 groupId 목록 수집
        List<Long> groupIds = partnerToMeReviews.stream()
                .map(gr -> gr.getReviewer().getGroup().getGroupId())
                .distinct()
                .toList();

        // 3. Tracker / UserBook 배치 조회 (N+1 방지)
        List<Tracker> trackers = trackerRepository.findByGroup_GroupIdIn(groupIds);
        Map<Long, Tracker> trackerByGroupId = trackers.stream()
                .collect(Collectors.toMap(t -> t.getGroup().getGroupId(), t -> t, (a, b) -> a));

        List<UserBook> userBooksInGroups = userBookRepository.findByGroup_GroupIdInWithUserAndGroup(groupIds);
        Map<String, UserBook> userBookByUserAndGroup = userBooksInGroups.stream()
                .collect(Collectors.toMap(
                        ub -> ub.getUser().getId() + "_" + ub.getGroup().getGroupId(),
                        ub -> ub,
                        (a, b) -> a
                ));

        // 4. 각 리뷰에 대해 맵에서 조회 후 DTO 생성
        List<GroupReviewResponseDTO.GroupReviewDetailDTO.MyReviewItemDTO> reviewItems = partnerToMeReviews.stream()
                .map(gr -> {
                    MatchedMember partnerMM = gr.getReviewer();
                    if (partnerMM == null) return null;

                    Groups group = partnerMM.getGroup();
                    Long groupId = group.getGroupId();
                    Long partnerUserId = partnerMM.getUser().getId();

                    Tracker tracker = trackerByGroupId.get(groupId);
                    UserBook partnerBookReview = userBookByUserAndGroup.get(partnerUserId + "_" + groupId);

                    return buildReviewItemDTO(group, tracker, partnerMM, gr, partnerBookReview);
                })
                .filter(Objects::nonNull)
                .toList();

        return GroupReviewResponseDTO.GroupReviewDetailDTO.builder()
                .reviews(reviewItems)
                .build();
    }

    private GroupReviewResponseDTO.GroupReviewDetailDTO.MyReviewItemDTO buildReviewItemDTO(
            Groups group, Tracker tracker, MatchedMember partnerMM, GroupReview gr, UserBook pub) {

        // 5. 뱃지 변환 로직
        List<GroupReviewResponseDTO.BadgeInfo> badges = (gr != null) ? gr.getBadges().stream()
                .map(b -> GroupReviewResponseDTO.BadgeInfo.builder()
                        .code(b.getBadge().name())
                        .description(b.getBadge().getDescription())
                        .build())
                .toList() : new ArrayList<>();

        return GroupReviewResponseDTO.GroupReviewDetailDTO.MyReviewItemDTO.builder()
                .groupId(group.getGroupId())
                .bookTitle(group.getBook().getTitle())
                .bookImage(group.getBook().getImage())
                .startDate(group.getStartDate().format(DATE_FMT))
                // 6. finishedDate 처리
                .finishedDate(tracker != null && tracker.getUpdatedAt() != null ?
                        tracker.getUpdatedAt().format(DATE_FMT) : "진행중")
                .partnerNickname(partnerMM != null ? partnerMM.getUser().getNickName() : "알 수 없음")
                // 7. DTO 필드명과 일치시킴 (partnerBadges)
                .partnerToMeRating(gr != null ? gr.getRating() : 0.0)
                .partnerToMeComment(gr != null ? gr.getComment() : "평가가 없습니다.")
                .partnerBadges(badges)
                // 8. 상대방 책 리뷰 (pub.getComment() 사용)
                .partnerBookRating(pub != null ? pub.getRating() : 0.0)
                .partnerBookComment(pub != null ? pub.getComment() : "리뷰가 없습니다.")
                .partnerBookReviewDate(pub != null && pub.getUpdatedAt() != null ?
                        pub.getUpdatedAt().format(DATE_FMT) : null)
                .build();
    }
}

