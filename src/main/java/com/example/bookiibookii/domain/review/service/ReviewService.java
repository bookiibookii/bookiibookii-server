package com.example.bookiibookii.domain.review.service;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.GroupType;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.exception.GroupException;
import com.example.bookiibookii.domain.group.exception.code.GroupErrorCode;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.groupbook.entity.GroupBook;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.review.dto.req.ReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.res.BookReviewResponseDTO;
import com.example.bookiibookii.domain.review.dto.res.GroupReviewResponseDTO;
import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.review.entity.GroupReview;
import com.example.bookiibookii.domain.review.exception.ReviewException;
import com.example.bookiibookii.domain.review.exception.code.ReviewErrorCode;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.review.repository.GroupReviewRepository;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
import com.example.bookiibookii.domain.tracker.repository.TrackerRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.groupbook.repository.GroupBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.example.bookiibookii.domain.tracker.enums.TrackerAction.REVIEW_DONE_CONFIRMED;

import java.time.format.DateTimeFormatter;
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

    private final GroupBookRepository groupBookRepository;
    private final TrackerRepository trackerRepository;
    private final MatchedMemberRepository matchedMemberRepository;
    private final BookReviewRepository bookReviewRepository;
    private final GroupReviewRepository groupReviewRepository;
    private final GroupsRepository groupsRepository;
    private final DomainEventPublisher publisher;

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
        MatchedMember me = getMatchedMember(group.getGroupId(), user.getId());
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

        updateExchangeStatusIfAllMembersReady(group.getGroupId(), group.getTradeType());

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
     * 1. [함께 읽기] 리뷰 생성
     * 파트너가 없으므로 본인의 서재(GroupBook)에 책 리뷰만 남깁니다.
     */
    @Transactional
    public void createTogetherReview(Long groupBookId, ReviewRequestDTO.TogetherReviewDTO request, User user) {
        validateRating(request.rating());
        validateCommentLength(request.comment(), BOOK_COMMENT_MAX_LENGTH);

        GroupBook groupBook = groupBookRepository.findById(groupBookId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.GROUP_BOOK_NOT_FOUND));

        if (!groupBook.getUser().getId().equals(user.getId())) {
            throw new ReviewException(ReviewErrorCode.NOT_GROUP_BOOK_OWNER);
        }

        groupBook.updateReview(request.rating(), request.comment());

        //그룹 조회 락 추가
        Groups group = groupsRepository.findByIdForUpdate(groupBook.getGroup().getGroupId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        // 그룹이 MATCHED 상태일 때만 종료 로직 수행
        if (group.getGroupStatus() == GroupStatus.MATCHED) {
            MatchedMember matchedMember = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(group.getGroupId(), user.getId())
                    .orElseThrow(() -> new ReviewException(ReviewErrorCode.MATCHED_MEMBER_NOT_FOUND));

            matchedMember.markReviewAsWritten();

            long remainingCount = matchedMemberRepository.countByGroup_GroupIdAndIsReviewWrittenFalse(group.getGroupId());

            if (remainingCount == 0) {
                group.updateStatus(GroupStatus.COMPLETED);
            }
        }
    }

    /*
     * 2. [릴레이 중간] 책 리뷰 생성 (1차/2차 독서 후, 교환·반납 전)
     * 책 리뷰를 저장하고 ReadingStatus를 REVIEW_DONE/REVIEW_DONE_2로 전환합니다.
     * 양측 모두 완료되면 TrackerStatus가 READ_DONE/READ_DONE_2로 자동 전환됩니다.

    @Transactional
    public void createMidRelayBookReview(Long groupBookId, ReviewRequestDTO.BookReviewDTO request, User user) {
        validateRating(request.bookRating());
        validateCommentLength(request.bookComment(), BOOK_COMMENT_MAX_LENGTH);

        GroupBook groupBook = groupBookRepository.findById(groupBookId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.GROUP_BOOK_NOT_FOUND));
        if (!groupBook.getUser().getId().equals(user.getId())) {
            throw new ReviewException(ReviewErrorCode.NOT_GROUP_BOOK_OWNER);
        }

        Long groupId = groupBook.getGroup().getGroupId();

        Tracker tracker = trackerRepository.findByGroupId(groupId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.TRACKER_NOT_FOUND));

        ReadingStatus readingStatus = tracker.getReadingStatus();
        if (readingStatus != ReadingStatus.MY_BOOK_READING && readingStatus != ReadingStatus.PARTNER_BOOK_READING) {
            throw new ReviewException(ReviewErrorCode.TRACKER_NOT_RETURNED); // 독서 단계가 아님
        }

        MatchedMember me = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, user.getId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.MATCHED_MEMBER_NOT_FOUND));

        Long partnerUserId = matchedMemberRepository.findPartnerUserId(groupId, user.getId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.PARTNER_NOT_FOUND));
        MatchedMember partner = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, partnerUserId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.PARTNER_NOT_FOUND));

        groupBook.updateReview(request.bookRating(), request.bookComment());

        if (readingStatus == ReadingStatus.MY_BOOK_READING) {
            if (me.getReadingStatus() != ReadingStatus.MY_BOOK_READ_DONE) {
                throw new ReviewException(ReviewErrorCode.TRACKER_NOT_RETURNED); // 독서 미완료
            }
            me.updateReadingStatus(ReadingStatus.MY_BOOK_REVIEW_DONE);
            if (partner.getReadingStatus() == ReadingStatus.MY_BOOK_REVIEW_DONE) {
                tracker.completeFirstReading(); // READING → READ_DONE
            }
        } else {
            if (me.getReadingStatus() != ReadingStatus.PARTNER_BOOK_READ_DONE) {
                throw new ReviewException(ReviewErrorCode.TRACKER_NOT_RETURNED); // 독서 미완료
            }
            me.updateReadingStatus(ReadingStatus.PARTNER_BOOK_REVIEW_DONE);
            if (partner.getReadingStatus() == ReadingStatus.PARTNER_BOOK_REVIEW_DONE) {
                tracker.completeSecondReading(); // READING_2 → READ_DONE_2
            }
        }

        publisher.publish(new TrackerNotificationEvent(REVIEW_DONE_CONFIRMED, user.getId(), groupId, null));
    } */

    /**
     * 3. [릴레이] 통합 리뷰 생성 (릴레이 종료 후)
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
        Groups group = groupsRepository.findByIdForUpdate(groupBook.getGroup().getGroupId())
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        // [보완] 그룹 상태가 MATCHED일 때만 리뷰 프로세스 진행
        if (group.getGroupStatus() != GroupStatus.MATCHED) {
            // 이미 COMPLETED거나 DELETED인 경우 예외를 던지거나 리턴 처리
            throw new GroupException(GroupErrorCode.INVALID_GROUP_STATUS);
        }

        Long groupId = group.getGroupId();
        ensureTrackerReturned(groupId);

        // 2-3. [책 리뷰] 업데이트
        groupBook.updateReview(request.bookRating(), request.bookComment());

        // 2-4. 리뷰어 조회 및 중복 체크
        MatchedMember reviewer = matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, user.getId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.MATCHED_MEMBER_NOT_FOUND));

        if (groupReviewRepository.existsByGroupIdAndReviewerUserId(groupId, user.getId())) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        // 2-5. 내 상태 업데이트
        reviewer.markReviewAsWritten(); // isReviewWritten = true

        // 2-6. 상대방 조회 및 리뷰 저장
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

        groupReviewRepository.save(groupReview);

        // [핵심] 락이 걸린 상태에서 전원 완료 여부 체크
        long remainingCount = matchedMemberRepository.countByGroup_GroupIdAndIsReviewWrittenFalse(groupId);

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
        return matchedMemberRepository.findByGroup_GroupIdAndUser_Id(groupId, userId)
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
        List<MatchedMember> matchedMembers = matchedMemberRepository.findAllByGroup_GroupId(groupId);
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
    }

    private ExchangeStatus resolveInitialExchangeStatus(TradeType tradeType) {
        if (tradeType == TradeType.DIRECT) {
            return ExchangeStatus.MEETING_SCHEDULE_WAITING;
        }
        return ExchangeStatus.TRACKING_REGISTER_WAITING;
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

        // 3. Tracker / GroupBook 배치 조회 (N+1 방지)
        List<Tracker> trackers = trackerRepository.findByGroup_GroupIdIn(groupIds);
        Map<Long, Tracker> trackerByGroupId = trackers.stream()
                .collect(Collectors.toMap(t -> t.getGroup().getGroupId(), t -> t, (a, b) -> a));

        List<GroupBook> groupBooksInGroups = groupBookRepository.findByGroup_GroupIdInWithUserAndGroup(groupIds);
        Map<String, GroupBook> groupBookByUserAndGroup = groupBooksInGroups.stream()
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
                    GroupBook partnerBookReview = groupBookByUserAndGroup.get(partnerUserId + "_" + groupId);

                    return buildReviewItemDTO(group, tracker, partnerMM, gr, partnerBookReview);
                })
                .filter(Objects::nonNull)
                .toList();

        return GroupReviewResponseDTO.GroupReviewDetailDTO.builder()
                .reviews(reviewItems)
                .build();
    }

    private GroupReviewResponseDTO.GroupReviewDetailDTO.MyReviewItemDTO buildReviewItemDTO(
            Groups group, Tracker tracker, MatchedMember partnerMM, GroupReview gr, GroupBook pub) {

        return GroupReviewResponseDTO.GroupReviewDetailDTO.MyReviewItemDTO.builder()
                .groupId(group.getGroupId())
                .bookTitle(group.getBook().getTitle())
                .bookImage(group.getBook().getImage())
                .startDate(group.getStartDate().format(DATE_FMT))
                // 6. finishedDate 처리
                .finishedDate(tracker != null && tracker.getUpdatedAt() != null ?
                        tracker.getUpdatedAt().format(DATE_FMT) : "진행중")
                .partnerNickname(partnerMM != null ? partnerMM.getUser().getNickName() : "알 수 없음")
                // 7. DTO 필드명과 일치시킴
                .partnerToMeRating(gr != null ? gr.getRating() : 0.0)
                .partnerToMeComment(gr != null ? gr.getComment() : "평가가 없습니다.")
                // 8. 상대방 책 리뷰 (pub.getComment() 사용)
                .partnerBookRating(pub != null ? pub.getRating() : 0.0)
                .partnerBookComment(pub != null ? pub.getComment() : "리뷰가 없습니다.")
                .partnerBookReviewDate(pub != null && pub.getUpdatedAt() != null ?
                        pub.getUpdatedAt().format(DATE_FMT) : null)
                .build();
    }
}
