package com.example.bookiibookii.domain.review.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.RoleStatus;
import com.example.bookiibookii.domain.group.repository.GroupsRepository;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.notification.enums.NotificationType;
import com.example.bookiibookii.domain.notification.publisher.DomainEventPublisher;
import com.example.bookiibookii.domain.review.dto.req.ReviewRequestDTO;
import com.example.bookiibookii.domain.review.dto.res.BookReviewResponseDTO;
import com.example.bookiibookii.domain.review.dto.res.MemberReviewResponseDTO;
import com.example.bookiibookii.domain.review.dto.res.MyBookReviewsResponseDTO;
import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.review.entity.MemberReview;
import com.example.bookiibookii.domain.review.enums.BookReviewType;
import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import com.example.bookiibookii.domain.review.exception.ReviewException;
import com.example.bookiibookii.domain.review.exception.code.ReviewErrorCode;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.review.repository.MemberReviewRepository;
import com.example.bookiibookii.domain.tracker.resolver.UserProfileImageUrlResolver;
import com.example.bookiibookii.domain.tracker.service.DeliveryAddressService;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import com.example.bookiibookii.domain.tracker.event.TrackerNotificationEvent;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    private static final Long GROUP_ID = 1L;

    @Mock
    private MatchedMemberRepository matchedMemberRepository;
    @Mock
    private BookReviewRepository bookReviewRepository;
    @Mock
    private MemberReviewRepository memberReviewRepository;
    @Mock
    private GroupsRepository groupsRepository;
    @Mock
    private DeliveryAddressService deliveryAddressService;
    @Mock
    private UserProfileImageUrlResolver userProfileImageUrlResolver;
    @Mock
    private DomainEventPublisher eventPublisher;

    private ReviewService reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewService(
                matchedMemberRepository,
                bookReviewRepository,
                memberReviewRepository,
                groupsRepository,
                deliveryAddressService,
                userProfileImageUrlResolver,
                eventPublisher,
                Clock.systemUTC()
        );
    }

    @Test
    void returnsEmptyReviewsWhenNoBookReviewWasWritten() {
        Fixture fixture = fixture();
        mockGroupMember(fixture);
        when(bookReviewRepository.findMyBookReviewsWithBook(fixture.member().getId(), GROUP_ID))
                .thenReturn(List.of());

        MyBookReviewsResponseDTO response = reviewService.getMyBookReviews(GROUP_ID, fixture.user());

        assertThat(response.reviews()).isEmpty();
        verifyNoInteractions(memberReviewRepository);
    }

    @Test
    void returnsOneReviewWhenOnlyMyOriginalBookWasReviewed() {
        Fixture fixture = fixture();
        BookReview myBookReview = review(fixture, 1000L, 100L, 20L, true, "내 책", 4.5);
        mockGroupMember(fixture);
        when(bookReviewRepository.findMyBookReviewsWithBook(fixture.member().getId(), GROUP_ID))
                .thenReturn(List.of(myBookReview));

        MyBookReviewsResponseDTO response = reviewService.getMyBookReviews(GROUP_ID, fixture.user());

        assertThat(response.reviews()).singleElement().satisfies(item -> {
            assertThat(item.reviewId()).isEqualTo(1000L);
            assertThat(item.reviewType()).isEqualTo(BookReviewType.MY_BOOK);
            assertThat(item.bookTitle()).isEqualTo("내 책");
            assertThat(item.bookAuthor()).isEqualTo("작가-20");
            assertThat(item.bookImageUrl()).isEqualTo("https://example.com/20.jpg");
            assertThat(item.rating()).isEqualTo(4.5);
            assertThat(item.content()).isEqualTo("내 책 후기");
            assertThat(item.isEditable()).isTrue();
        });
    }

    @Test
    void returnsBothReviewsInsteadOfOnlyTheLatestOne() {
        Fixture fixture = fixture();
        BookReview myBookReview = review(fixture, 1000L, 100L, 20L, true, "내 책", 4.0);
        BookReview partnerBookReview = review(fixture, 1001L, 101L, 21L, false, "파트너 책", 5.0);
        mockGroupMember(fixture);
        when(bookReviewRepository.findMyBookReviewsWithBook(fixture.member().getId(), GROUP_ID))
                .thenReturn(List.of(myBookReview, partnerBookReview));

        MyBookReviewsResponseDTO response = reviewService.getMyBookReviews(GROUP_ID, fixture.user());

        assertThat(response.reviews()).hasSize(2);
        assertThat(response.reviews())
                .extracting(MyBookReviewsResponseDTO.BookReviewItem::reviewId)
                .containsExactly(1000L, 1001L);
        assertThat(response.reviews())
                .extracting(MyBookReviewsResponseDTO.BookReviewItem::reviewType)
                .containsExactly(BookReviewType.MY_BOOK, BookReviewType.PARTNER_BOOK);
        verifyNoInteractions(memberReviewRepository);
    }

    @Test
    void queriesOnlyCurrentMembersReviewsSoAnotherUsersReviewIsNotIncluded() {
        Fixture fixture = fixture();
        BookReview myReview = review(fixture, 1000L, 100L, 20L, true, "내 책", 4.5);
        mockGroupMember(fixture);
        when(bookReviewRepository.findMyBookReviewsWithBook(10L, GROUP_ID))
                .thenReturn(List.of(myReview));

        MyBookReviewsResponseDTO response = reviewService.getMyBookReviews(GROUP_ID, fixture.user());

        assertThat(response.reviews()).extracting(MyBookReviewsResponseDTO.BookReviewItem::reviewId)
                .containsExactly(1000L);
        verify(bookReviewRepository).findMyBookReviewsWithBook(10L, GROUP_ID);
        verify(bookReviewRepository, never()).findMyBookReviewsWithBook(20L, GROUP_ID);
    }

    @Test
    void rejectsUserWhoIsNotGroupMember() {
        User outsider = user(99L);
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(GROUP_ID, 99L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getMyBookReviews(GROUP_ID, outsider))
                .isInstanceOfSatisfying(ReviewException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ReviewErrorCode.NOT_GROUP_MEMBER));

        verify(bookReviewRepository, never()).findMyBookReviewsWithBook(10L, GROUP_ID);
    }

    @Test
    void updatesMyBookReviewByReviewId() {
        Fixture fixture = fixture();
        BookReview bookReview = review(fixture, 1001L, 101L, 21L, false, "파트너 책", 4.0);
        ReviewRequestDTO.BookReviewUpsertDTO request =
                new ReviewRequestDTO.BookReviewUpsertDTO(5.0, "수정된 후기");
        mockGroupMember(fixture);
        when(bookReviewRepository.findByIdAndMatchedMember_IdAndMatchedMember_Group_Id(1001L, 10L, GROUP_ID))
                .thenReturn(Optional.of(bookReview));

        BookReviewResponseDTO response =
                reviewService.updateMyBookReview(GROUP_ID, 1001L, request, fixture.user());

        assertThat(response.reviewId()).isEqualTo(1001L);
        assertThat(response.star()).isEqualTo(5.0);
        assertThat(response.comment()).isEqualTo("수정된 후기");
        assertThat(bookReview.getStar()).isEqualTo(5.0);
        assertThat(bookReview.getComment()).isEqualTo("수정된 후기");
    }

    @Test
    void cannotUpdateAnotherUsersOrAnotherGroupsBookReview() {
        Fixture fixture = fixture();
        ReviewRequestDTO.BookReviewUpsertDTO request =
                new ReviewRequestDTO.BookReviewUpsertDTO(5.0, "수정 시도");
        mockGroupMember(fixture);
        when(bookReviewRepository.findByIdAndMatchedMember_IdAndMatchedMember_Group_Id(2000L, 10L, GROUP_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.updateMyBookReview(GROUP_ID, 2000L, request, fixture.user()))
                .isInstanceOfSatisfying(ReviewException.class,
                        exception -> assertThat(exception.getCode()).isEqualTo(ReviewErrorCode.BOOK_REVIEW_NOT_FOUND));
    }

    @Test
    void firstPartnerReviewDoesNotCompleteReading() {
        PartnerReviewFixture fixture = partnerReviewFixture();
        mockPartnerReviewCreation(fixture, false);

        MemberReviewResponseDTO response = reviewService.createMemberReview(
                GROUP_ID,
                new ReviewRequestDTO.MemberReviewCreateDTO(MemberReviewReaction.BOOM_UP, "좋았어요"),
                fixture.me().getUser()
        );

        assertThat(response.groupCompleted()).isFalse();
        assertThat(fixture.group().getGroupStatus()).isEqualTo(GroupStatus.MATCHED);
        assertThat(fixture.me().getReadingStatus()).isEqualTo(ReadingStatus.PARTNER_REVIEWING);
        assertThat(fixture.partner().getReadingStatus()).isEqualTo(ReadingStatus.PARTNER_REVIEWING);
        assertThat(fixture.me().getCompletedAt()).isNull();
        assertThat(fixture.partner().getCompletedAt()).isNull();
        assertThat(fixture.me().isReviewWritten()).isTrue();
        assertThat(fixture.partner().isReviewWritten()).isFalse();
        assertThat(fixture.me().getCurrentMemberBook().getCurrentPage()).isEqualTo(87);
        assertThat(fixture.partner().getCurrentMemberBook().getCurrentPage()).isEqualTo(93);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        TrackerNotificationEvent event = (TrackerNotificationEvent) captor.getValue();
        assertThat(event.notificationType()).isEqualTo(NotificationType.TRACKER_EXCHANGE_REVIEW_CREATED);
        assertThat(event.receiverIds()).containsExactly(fixture.partner().getUser().getId());
    }

    @Test
    void secondPartnerReviewCompletesReadingAndStoresCompletedAt() {
        PartnerReviewFixture fixture = partnerReviewFixture();
        fixture.partner().markReviewAsWritten();
        mockPartnerReviewCreation(fixture, true);

        MemberReviewResponseDTO response = reviewService.createMemberReview(
                GROUP_ID,
                new ReviewRequestDTO.MemberReviewCreateDTO(MemberReviewReaction.BOOM_UP, "좋았어요"),
                fixture.me().getUser()
        );

        assertThat(response.groupCompleted()).isTrue();
        assertThat(fixture.me().getReadingStatus()).isEqualTo(ReadingStatus.COMPLETED);
        assertThat(fixture.partner().getReadingStatus()).isEqualTo(ReadingStatus.COMPLETED);
        assertThat(fixture.me().getCompletedAt()).isNotNull();
        assertThat(fixture.partner().getCompletedAt()).isEqualTo(fixture.me().getCompletedAt());
        assertThat(fixture.group().getGroupStatus()).isEqualTo(GroupStatus.COMPLETED);
        assertThat(fixture.me().getCurrentMemberBook().getCurrentPage()).isEqualTo(87);
        assertThat(fixture.partner().getCurrentMemberBook().getCurrentPage()).isEqualTo(93);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        TrackerNotificationEvent event = (TrackerNotificationEvent) captor.getValue();
        assertThat(event.notificationType()).isEqualTo(NotificationType.TRACKER_EXCHANGE_COMPLETED);
        assertThat(event.receiverIds())
                .containsExactly(fixture.me().getUser().getId(), fixture.partner().getUser().getId());
    }

    @Test
    void hostCompletingFirstReadingPublishesNotificationForGuest() {
        BookReviewTransitionFixture fixture = bookReviewTransitionFixture(
                ReadingStatus.MY_BOOK_REVIEWING,
                RoleStatus.HOST
        );
        mockBookReviewTransition(fixture);

        reviewService.createBookReview(
                GROUP_ID,
                new ReviewRequestDTO.BookReviewUpsertDTO(4.5, "책 후기"),
                fixture.me().getUser()
        );

        assertThat(fixture.me().getReadingStatus()).isEqualTo(ReadingStatus.EXCHANGING);
        assertThat(fixture.partner().getReadingStatus()).isEqualTo(ReadingStatus.EXCHANGING);
        assertBookReviewNotification(fixture, ExchangeRound.FIRST_EXCHANGE);
    }

    @Test
    void guestCompletingFirstReadingPublishesNotificationForHost() {
        BookReviewTransitionFixture fixture = bookReviewTransitionFixture(
                ReadingStatus.MY_BOOK_REVIEWING,
                RoleStatus.GUEST
        );
        mockBookReviewTransition(fixture);

        reviewService.createBookReview(
                GROUP_ID,
                new ReviewRequestDTO.BookReviewUpsertDTO(4.5, "책 후기"),
                fixture.me().getUser()
        );

        assertBookReviewNotification(fixture, ExchangeRound.FIRST_EXCHANGE);
    }

    @Test
    void hostCompletingSecondReadingPublishesNotificationForGuest() {
        BookReviewTransitionFixture fixture = bookReviewTransitionFixture(
                ReadingStatus.PARTNER_BOOK_REVIEWING,
                RoleStatus.HOST
        );
        mockBookReviewTransition(fixture);

        reviewService.createBookReview(
                GROUP_ID,
                new ReviewRequestDTO.BookReviewUpsertDTO(4.5, "책 후기"),
                fixture.me().getUser()
        );

        assertThat(fixture.me().getReadingStatus()).isEqualTo(ReadingStatus.RETURNING);
        assertThat(fixture.partner().getReadingStatus()).isEqualTo(ReadingStatus.RETURNING);
        assertBookReviewNotification(fixture, ExchangeRound.RETURN_EXCHANGE);
    }

    @Test
    void guestCompletingSecondReadingPublishesNotificationForHost() {
        BookReviewTransitionFixture fixture = bookReviewTransitionFixture(
                ReadingStatus.PARTNER_BOOK_REVIEWING,
                RoleStatus.GUEST
        );
        mockBookReviewTransition(fixture);

        reviewService.createBookReview(
                GROUP_ID,
                new ReviewRequestDTO.BookReviewUpsertDTO(4.5, "책 후기"),
                fixture.me().getUser()
        );

        assertBookReviewNotification(fixture, ExchangeRound.RETURN_EXCHANGE);
    }

    @Test
    void reviewBelowFullProgressDoesNotPublishRoundCompletionNotification() {
        BookReviewTransitionFixture fixture = bookReviewTransitionFixture(
                ReadingStatus.MY_BOOK_REVIEWING,
                RoleStatus.HOST
        );
        fixture.me().getCurrentMemberBook().updateCurrentPage(99);
        mockBookReviewSave(fixture);

        reviewService.createBookReview(
                GROUP_ID,
                new ReviewRequestDTO.BookReviewUpsertDTO(4.5, "책 후기"),
                fixture.me().getUser()
        );

        assertThat(fixture.me().getReadingStatus()).isEqualTo(ReadingStatus.MY_BOOK_REVIEWING);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void firstPartnerToFinishRoundIsNotBlockedByGroupStateTransition() {
        BookReviewTransitionFixture fixture = bookReviewTransitionFixture(
                ReadingStatus.MY_BOOK_REVIEWING,
                RoleStatus.HOST
        );
        mockBookReviewSave(fixture);
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(
                fixture.partner().getId(),
                fixture.partner().getCurrentMemberBook().getId()
        )).thenReturn(false);

        reviewService.createBookReview(
                GROUP_ID,
                new ReviewRequestDTO.BookReviewUpsertDTO(4.5, "책 후기"),
                fixture.me().getUser()
        );

        assertThat(fixture.me().getReadingStatus()).isEqualTo(ReadingStatus.MY_BOOK_REVIEWING);
        assertBookReviewNotification(fixture, ExchangeRound.FIRST_EXCHANGE);
    }

    @Test
    void partnerReviewStageDoesNotAllowAnotherBookReview() {
        PartnerReviewFixture fixture = partnerReviewFixture();
        MemberBook currentBook = memberBook(fixture.me(), 500L, "파트너 책");
        ReflectionTestUtils.setField(fixture.me(), "currentMemberBook", currentBook);
        when(groupsRepository.findByIdForUpdate(GROUP_ID)).thenReturn(Optional.of(fixture.group()));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(GROUP_ID))
                .thenReturn(List.of(fixture.me(), fixture.partner()));

        assertThatThrownBy(() -> reviewService.createBookReview(
                GROUP_ID,
                new ReviewRequestDTO.BookReviewUpsertDTO(4.5, "책 후기"),
                fixture.me().getUser()
        )).isInstanceOfSatisfying(ReviewException.class,
                exception -> assertThat(exception.getCode())
                        .isEqualTo(ReviewErrorCode.INVALID_REVIEW_READING_STATUS));

        verifyNoInteractions(memberReviewRepository);
    }

    private void mockPartnerReviewCreation(PartnerReviewFixture fixture, boolean partnerAlreadyReviewed) {
        when(groupsRepository.findByIdForUpdate(GROUP_ID)).thenReturn(Optional.of(fixture.group()));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(GROUP_ID))
                .thenReturn(List.of(fixture.me(), fixture.partner()));
        when(memberReviewRepository.existsByGroup_IdAndWriter_Id(GROUP_ID, fixture.me().getId()))
                .thenReturn(false);
        when(memberReviewRepository.saveAndFlush(any(MemberReview.class)))
                .thenAnswer(invocation -> {
                    MemberReview review = invocation.getArgument(0);
                    ReflectionTestUtils.setField(review, "id", 900L);
                    return review;
                });
        when(memberReviewRepository.existsByGroup_IdAndWriter_Id(GROUP_ID, fixture.partner().getId()))
                .thenReturn(partnerAlreadyReviewed);
    }


    private PartnerReviewFixture partnerReviewFixture() {
        Groups group = Groups.builder()
                .id(GROUP_ID)
                .groupStatus(GroupStatus.MATCHED)
                .tradeType(com.example.bookiibookii.domain.group.enums.TradeType.DIRECT)
                .book(Book.builder().id(1L).title("교환 책").build())
                .build();
        MatchedMember me = MatchedMember.builder()
                .id(10L)
                .group(group)
                .user(user(1L))
                .readingStatus(ReadingStatus.PARTNER_REVIEWING)
                .exchangeStatus(ExchangeStatus.NOT_STARTED)
                .build();
        MatchedMember partner = MatchedMember.builder()
                .id(20L)
                .group(group)
                .user(user(2L))
                .readingStatus(ReadingStatus.PARTNER_REVIEWING)
                .exchangeStatus(ExchangeStatus.NOT_STARTED)
                .build();
        MemberBook myBook = memberBook(me, 700L, "돌려받은 내 책");
        MemberBook partnerBook = memberBook(partner, 701L, "돌려받은 상대 책");
        myBook.updateCurrentPage(87);
        partnerBook.updateCurrentPage(93);
        ReflectionTestUtils.setField(me, "currentMemberBook", myBook);
        ReflectionTestUtils.setField(partner, "currentMemberBook", partnerBook);
        return new PartnerReviewFixture(group, me, partner);
    }

    private BookReviewTransitionFixture bookReviewTransitionFixture(
            ReadingStatus status,
            RoleStatus actorRole
    ) {
        Groups group = Groups.builder()
                .id(GROUP_ID)
                .groupStatus(GroupStatus.MATCHED)
                .tradeType(com.example.bookiibookii.domain.group.enums.TradeType.DIRECT)
                .book(Book.builder().id(1L).title("교환 책").build())
                .build();
        MatchedMember me = MatchedMember.builder()
                .id(10L)
                .group(group)
                .user(user(1L))
                .role(actorRole)
                .readingStatus(status)
                .build();
        MatchedMember partner = MatchedMember.builder()
                .id(20L)
                .group(group)
                .user(user(2L))
                .role(actorRole == RoleStatus.HOST ? RoleStatus.GUEST : RoleStatus.HOST)
                .readingStatus(status)
                .build();
        ReflectionTestUtils.setField(me, "currentMemberBook", memberBook(me, 500L, "내가 읽은 책"));
        ReflectionTestUtils.setField(partner, "currentMemberBook", memberBook(partner, 600L, "상대가 읽은 책"));
        return new BookReviewTransitionFixture(group, me, partner);
    }

    private void mockBookReviewTransition(BookReviewTransitionFixture fixture) {
        mockBookReviewSave(fixture);
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(
                fixture.partner().getId(),
                fixture.partner().getCurrentMemberBook().getId()
        )).thenReturn(true);
        when(matchedMemberRepository.findAllByGroup_Id(GROUP_ID))
                .thenReturn(List.of(fixture.me(), fixture.partner()));
    }

    private void mockBookReviewSave(BookReviewTransitionFixture fixture) {
        when(groupsRepository.findByIdForUpdate(GROUP_ID)).thenReturn(Optional.of(fixture.group()));
        when(matchedMemberRepository.findAllByGroupIdForUpdate(GROUP_ID))
                .thenReturn(List.of(fixture.me(), fixture.partner()));
        when(bookReviewRepository.existsByMatchedMember_IdAndMemberBook_Id(
                fixture.me().getId(),
                fixture.me().getCurrentMemberBook().getId()
        )).thenReturn(false, true);
        when(bookReviewRepository.existsByMemberBookId(fixture.me().getCurrentMemberBook().getId()))
                .thenReturn(false);
        when(bookReviewRepository.saveAndFlush(any(BookReview.class)))
                .thenAnswer(invocation -> {
                    BookReview review = invocation.getArgument(0);
                    ReflectionTestUtils.setField(review, "id", 800L);
                    return review;
                });
    }

    private void assertBookReviewNotification(BookReviewTransitionFixture fixture, ExchangeRound exchangeRound) {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());
        TrackerNotificationEvent event = (TrackerNotificationEvent) captor.getValue();
        assertThat(event.notificationType()).isEqualTo(NotificationType.TRACKER_READING_REVIEW_COMPLETED);
        assertThat(event.receiverIds()).containsExactly(fixture.partner().getUser().getId());
        assertThat(event.exchangeRound()).isEqualTo(exchangeRound);
    }

    private MemberBook memberBook(MatchedMember member, Long id, String title) {
        return MemberBook.builder()
                .id(id)
                .group(member.getGroup())
                .matchedMember(member)
                .book(Book.builder().id(id).title(title).totalPages(100).build())
                .isMine(false)
                .currentPage(100)
                .build();
    }

    private void mockGroupMember(Fixture fixture) {
        when(matchedMemberRepository.findByGroup_IdAndUser_Id(GROUP_ID, fixture.user().getId()))
                .thenReturn(Optional.of(fixture.member()));
    }

    private Fixture fixture() {
        User user = user(1L);
        Groups group = Groups.builder().id(GROUP_ID).build();
        MatchedMember member = MatchedMember.builder()
                .id(10L)
                .group(group)
                .user(user)
                .build();
        return new Fixture(user, group, member);
    }

    private BookReview review(
            Fixture fixture,
            Long reviewId,
            Long memberBookId,
            Long bookId,
            boolean isMine,
            String title,
            Double rating
    ) {
        Book book = Book.builder()
                .id(bookId)
                .isbn13("9780000000" + bookId)
                .title(title)
                .author("작가-" + bookId)
                .publisher("출판사")
                .image("https://example.com/" + bookId + ".jpg")
                .totalPages(100)
                .link("https://example.com/books/" + bookId)
                .build();
        MemberBook memberBook = MemberBook.builder()
                .id(memberBookId)
                .group(fixture.group())
                .book(book)
                .matchedMember(fixture.member())
                .isMine(isMine)
                .build();
        BookReview review = BookReview.builder()
                .id(reviewId)
                .matchedMember(fixture.member())
                .memberBook(memberBook)
                .star(rating)
                .comment(title + " 후기")
                .build();
        ReflectionTestUtils.setField(review, "createdAt", Instant.parse("2026-06-08T01:00:00Z"));
        ReflectionTestUtils.setField(review, "updatedAt", Instant.parse("2026-06-08T02:00:00Z"));
        return review;
    }

    private User user(Long id) {
        return User.builder()
                .id(id)
                .nickName("user-" + id)
                .socialType(SocialType.KAKAO)
                .socialId("social-" + id)
                .build();
    }

    private record Fixture(User user, Groups group, MatchedMember member) {
    }

    private record PartnerReviewFixture(Groups group, MatchedMember me, MatchedMember partner) {
    }

    private record BookReviewTransitionFixture(Groups group, MatchedMember me, MatchedMember partner) {
    }

}
