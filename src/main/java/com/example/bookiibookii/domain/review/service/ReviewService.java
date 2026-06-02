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
import com.example.bookiibookii.domain.review.dto.req.ReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.res.BookReviewResponseDTO;
import com.example.bookiibookii.domain.review.dto.res.GroupReviewsResponseDTO;
import com.example.bookiibookii.domain.review.dto.res.MemberReviewResponseDTO;
import com.example.bookiibookii.domain.review.dto.res.MyGroupReviewsResponseDTO;
import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.review.entity.MemberReview;
import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import com.example.bookiibookii.domain.review.exception.ReviewException;
import com.example.bookiibookii.domain.review.exception.code.ReviewErrorCode;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.review.repository.MemberReviewRepository;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.service.DeliveryAddressService;
import com.example.bookiibookii.domain.tracker.resolver.UserProfileImageUrlResolver;
import com.example.bookiibookii.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private static final double RATING_MIN = 0.0;
    private static final double RATING_MAX = 5.0;
    private static final int BOOK_COMMENT_MAX_LENGTH = 500;
    private static final int MEMBER_COMMENT_MAX_LENGTH = 20;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy. MM. dd.");

    private final MatchedMemberRepository matchedMemberRepository;
    private final BookReviewRepository bookReviewRepository;
    private final MemberReviewRepository memberReviewRepository;
    private final GroupsRepository groupsRepository;
    private final DeliveryAddressService deliveryAddressService;
    private final UserProfileImageUrlResolver userProfileImageUrlResolver;

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
        List<MatchedMember> members = matchedMemberRepository.findAllByGroupIdForUpdate(group.getId());
        validatePairGroup(members);

        MatchedMember me = findMe(members, user.getId());
        MemberBook currentMemberBook = getCurrentMemberBook(me);
        ReadingStatus currentStatus = validateBookReviewReadingStatus(me.getReadingStatus());

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
            bookReview = bookReviewRepository.saveAndFlush(bookReview);
        } catch (DataIntegrityViolationException e) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        updateReadingStatusIfAllBookReviewsWritten(group.getId(), group.getTradeType(), members, currentStatus);

        return BookReviewResponseDTO.from(bookReview);
    }

    @Transactional
    public MemberReviewResponseDTO createMemberReview(
            Long groupId,
            ReviewRequestDTO.MemberReviewCreateDTO request,
            User user
    ) {
        validateCommentRequired(request.comment(), MEMBER_COMMENT_MAX_LENGTH);

        Groups group = groupsRepository.findByIdForUpdate(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));
        List<MatchedMember> members = matchedMemberRepository.findAllByGroupIdForUpdate(group.getId());
        validatePairGroup(members);

        MatchedMember me = findMe(members, user.getId());
        MatchedMember partner = findPartner(members, me.getId());

        validateSecondExchangeCompleted(members);

        if (memberReviewRepository.existsByGroup_IdAndWriter_Id(group.getId(), me.getId())) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        MemberReview memberReview = MemberReview.create(
                group,
                me,
                partner,
                request.reaction(),
                request.comment()
        );

        try {
            memberReview = memberReviewRepository.saveAndFlush(memberReview);
        } catch (DataIntegrityViolationException e) {
            throw new ReviewException(ReviewErrorCode.REVIEW_ALREADY_EXISTS);
        }

        boolean partnerAlreadyReviewed = memberReviewRepository.existsByGroup_IdAndWriter_Id(group.getId(), partner.getId());
        if (partnerAlreadyReviewed) {
            members.forEach(member -> member.updateReadingStatus(ReadingStatus.COMPLETED));
            group.updateStatus(GroupStatus.COMPLETED);
        }

        return MemberReviewResponseDTO.builder()
                .reviewId(memberReview.getId())
                .groupCompleted(partnerAlreadyReviewed)
                .build();
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

    @Transactional
    public MyGroupReviewsResponseDTO updateMyGroupReviews(
            Long groupId,
            ReviewRequestDTO.MyGroupReviewsUpdateDTO request,
            User user
    ) {
        validateCompletedGroupReviewAccess(groupId, user.getId());
        MatchedMember me = getMatchedMember(groupId, user.getId());
        validateHasUpdates(request);

        if (request.bookReviews() != null) {
            for (ReviewRequestDTO.MyGroupReviewsUpdateDTO.BookReviewUpdateItem item : request.bookReviews()) {
                updateMyBookReviewItem(me, item);
            }
        }

        if (request.memberReview() != null) {
            updateMyMemberReview(groupId, me, request.memberReview());
        }

        return buildMyGroupReviewsResponse(me.getId(), groupId);
    }

    @Transactional(readOnly = true)
    public GroupReviewsResponseDTO getGroupReviews(Long groupId, User user) {
        validateCompletedGroupReviewAccess(groupId, user.getId());

        List<GroupReviewsResponseDTO.BookReviewItem> bookReviews = bookReviewRepository
                .findAllByGroupIdWithDetails(groupId)
                .stream()
                .map(this::toBookReviewItem)
                .toList();

        List<GroupReviewsResponseDTO.MemberReviewItem> memberReviews = memberReviewRepository
                .findAllByGroupIdWithDetails(groupId)
                .stream()
                .map(this::toMemberReviewItem)
                .toList();

        return GroupReviewsResponseDTO.builder()
                .bookReviews(bookReviews)
                .memberReviews(memberReviews)
                .build();
    }

    private void validateCompletedGroupReviewAccess(Long groupId, Long userId) {
        Groups group = groupsRepository.findByIdWithBookAndHost(groupId)
                .orElseThrow(() -> new GroupException(GroupErrorCode.GROUP_NOT_FOUND));

        if (!matchedMemberRepository.existsByGroup_IdAndUser_Id(groupId, userId)) {
            throw new ReviewException(ReviewErrorCode.NOT_GROUP_MEMBER);
        }

        if (group.getGroupStatus() != GroupStatus.COMPLETED) {
            throw new ReviewException(ReviewErrorCode.GROUP_REVIEW_NOT_AVAILABLE);
        }
    }

    private void validateHasUpdates(ReviewRequestDTO.MyGroupReviewsUpdateDTO request) {
        boolean hasBookUpdates = request.bookReviews() != null
                && request.bookReviews().stream().anyMatch(this::hasBookReviewChanges);

        boolean hasMemberUpdate = request.memberReview() != null
                && (request.memberReview().reaction() != null || request.memberReview().comment() != null);

        if (!hasBookUpdates && !hasMemberUpdate) {
            throw new ReviewException(ReviewErrorCode.NOTHING_TO_UPDATE);
        }
    }

    private boolean hasBookReviewChanges(ReviewRequestDTO.MyGroupReviewsUpdateDTO.BookReviewUpdateItem item) {
        return item.star() != null || item.comment() != null;
    }

    private void updateMyBookReviewItem(
            MatchedMember me,
            ReviewRequestDTO.MyGroupReviewsUpdateDTO.BookReviewUpdateItem item
    ) {
        if (!hasBookReviewChanges(item)) {
            throw new ReviewException(ReviewErrorCode.NOTHING_TO_UPDATE);
        }

        BookReview bookReview = bookReviewRepository
                .findByMatchedMember_IdAndMemberBook_Id(me.getId(), item.memberBookId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.BOOK_REVIEW_NOT_FOUND));

        if (!bookReview.getMemberBook().isOwnedBy(me)) {
            throw new ReviewException(ReviewErrorCode.BOOK_REVIEW_NOT_FOUND);
        }

        Double newStar = item.star() != null ? item.star() : bookReview.getStar();
        String newComment = item.comment() != null ? item.comment() : bookReview.getComment();

        if (item.star() != null) {
            validateRating(item.star());
        }
        if (item.comment() != null) {
            validateCommentLength(item.comment(), BOOK_COMMENT_MAX_LENGTH);
        }

        bookReview.updateReview(newStar, newComment);
    }

    private void updateMyMemberReview(
            Long groupId,
            MatchedMember me,
            ReviewRequestDTO.MyGroupReviewsUpdateDTO.MemberReviewUpdateItem item
    ) {
        if (item.reaction() == null && item.comment() == null) {
            throw new ReviewException(ReviewErrorCode.NOTHING_TO_UPDATE);
        }

        MemberReview memberReview = memberReviewRepository
                .findByGroupIdAndWriterIdWithDetails(groupId, me.getId())
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.MEMBER_REVIEW_NOT_FOUND));

        MemberReviewReaction newReaction = item.reaction() != null
                ? item.reaction()
                : memberReview.getReaction();
        String newComment = item.comment() != null
                ? item.comment()
                : memberReview.getComment();

        if (item.comment() != null) {
            validateCommentRequired(item.comment(), MEMBER_COMMENT_MAX_LENGTH);
        }

        memberReview.updateReview(newReaction, newComment);
    }

    private MyGroupReviewsResponseDTO buildMyGroupReviewsResponse(Long matchedMemberId, Long groupId) {
        List<GroupReviewsResponseDTO.BookReviewItem> bookReviews = bookReviewRepository
                .findAllByMatchedMemberIdAndGroupIdWithDetails(matchedMemberId, groupId)
                .stream()
                .map(this::toBookReviewItem)
                .toList();

        GroupReviewsResponseDTO.MemberReviewItem memberReview = memberReviewRepository
                .findByGroupIdAndWriterIdWithDetails(groupId, matchedMemberId)
                .map(this::toMemberReviewItem)
                .orElse(null);

        return MyGroupReviewsResponseDTO.builder()
                .bookReviews(bookReviews)
                .memberReview(memberReview)
                .build();
    }

    private GroupReviewsResponseDTO.BookReviewItem toBookReviewItem(BookReview bookReview) {
        var book = bookReview.getMemberBook().getBook();
        var writer = bookReview.getMatchedMember().getUser();

        return GroupReviewsResponseDTO.BookReviewItem.builder()
                .bookId(book.getId())
                .bookTitle(book.getTitle())
                .bookAuthor(book.getAuthor())
                .bookImage(book.getImage())
                .writerId(writer.getId())
                .writerNickname(writer.getNickName())
                .writerProfileImageUrl(userProfileImageUrlResolver.resolve(writer))
                .star(bookReview.getStar())
                .comment(bookReview.getComment())
                .createdAt(bookReview.getCreatedAt().format(DATE_FMT))
                .build();
    }

    private GroupReviewsResponseDTO.MemberReviewItem toMemberReviewItem(MemberReview memberReview) {
        var group = memberReview.getGroup();
        var writer = memberReview.getWriter().getUser();

        return GroupReviewsResponseDTO.MemberReviewItem.builder()
                .groupName(group.getGroupName())
                .readingPeriod(group.getReadingPeriod())
                .writerId(writer.getId())
                .writerNickname(writer.getNickName())
                .writerProfileImageUrl(userProfileImageUrlResolver.resolve(writer))
                .reaction(memberReview.getReaction())
                .comment(memberReview.getComment())
                .build();
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

    private void validateCommentRequired(String comment, int limit) {
        if (comment == null || comment.isBlank()) {
            throw new ReviewException(ReviewErrorCode.COMMENT_REQUIRED);
        }
        validateCommentLength(comment, limit);
    }

    private MatchedMember getMatchedMember(Long groupId, Long userId) {
        return matchedMemberRepository.findByGroup_IdAndUser_Id(groupId, userId)
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.NOT_GROUP_MEMBER));
    }

    private ReadingStatus validateBookReviewReadingStatus(ReadingStatus readingStatus) {
        if (readingStatus == ReadingStatus.MY_BOOK_REVIEWING
                || readingStatus == ReadingStatus.PARTNER_BOOK_REVIEWING) {
            return readingStatus;
        }
        throw new ReviewException(ReviewErrorCode.INVALID_REVIEW_READING_STATUS);
    }

    private void updateReadingStatusIfAllBookReviewsWritten(
            Long groupId,
            TradeType tradeType,
            List<MatchedMember> members,
            ReadingStatus reviewStatus
    ) {
        boolean allMembersInSameReviewStatus = members.stream()
                .allMatch(member -> member.getReadingStatus() == reviewStatus);
        boolean allCurrentBookReviewsWritten = members.stream()
                .allMatch(this::existsCurrentBookReview);

        if (!allMembersInSameReviewStatus || !allCurrentBookReviewsWritten) {
            return;
        }

        ReadingStatus nextStatus = reviewStatus == ReadingStatus.MY_BOOK_REVIEWING
                ? ReadingStatus.EXCHANGING
                : ReadingStatus.RETURNING;
        members.forEach(member -> member.updateReadingStatus(nextStatus));
        updateExchangeStatusIfAllMembersReady(groupId, tradeType);
    }

    private boolean existsCurrentBookReview(MatchedMember matchedMember) {
        MemberBook currentMemberBook = getCurrentMemberBook(matchedMember);
        return bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(
                matchedMember.getId(),
                currentMemberBook.getId()
        );
    }

    private void validatePairGroup(List<MatchedMember> matchedMembers) {
        if (matchedMembers.size() != 2) {
            throw new ReviewException(ReviewErrorCode.MATCHED_MEMBER_NOT_FOUND);
        }
    }

    private MatchedMember findMe(List<MatchedMember> matchedMembers, Long userId) {
        return matchedMembers.stream()
                .filter(member -> member.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.NOT_GROUP_MEMBER));
    }

    private MatchedMember findPartner(List<MatchedMember> matchedMembers, Long myMatchedMemberId) {
        return matchedMembers.stream()
                .filter(member -> !member.getId().equals(myMatchedMemberId))
                .findFirst()
                .orElseThrow(() -> new ReviewException(ReviewErrorCode.PARTNER_MEMBER_NOT_FOUND));
    }

    private void validateSecondExchangeCompleted(List<MatchedMember> matchedMembers) {
        boolean returnExchangeCompleted = matchedMembers.stream()
                .allMatch(member -> member.getReadingStatus() == ReadingStatus.RETURNING
                        && member.getExchangeStatus() == ExchangeStatus.NOT_STARTED);
        if (!returnExchangeCompleted) {
            throw new ReviewException(ReviewErrorCode.INVALID_MEMBER_REVIEW_STATUS);
        }
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
