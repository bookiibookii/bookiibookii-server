package com.example.bookiibookii.domain.review.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.review.dto.res.MypageReviewResponseDTO;
import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.review.entity.MemberReview;
import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.review.repository.MemberReviewRepository;
import com.example.bookiibookii.domain.tracker.resolver.UserProfileImageUrlResolver;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MypageReviewServiceTest {

    private static final Long USER_ID = 1L;

    @Mock
    private BookReviewRepository bookReviewRepository;
    @Mock
    private MemberReviewRepository memberReviewRepository;
    @Mock
    private UserProfileImageUrlResolver userProfileImageUrlResolver;

    private MypageReviewService mypageReviewService;

    @BeforeEach
    void setUp() {
        mypageReviewService = new MypageReviewService(
                bookReviewRepository,
                memberReviewRepository,
                userProfileImageUrlResolver
        );
    }

    @Test
    void writtenReviewsReturnOnlyBookReviewsAndExcludeWrittenPartnerReviews() {
        PageRequest pageable = PageRequest.of(0, 20);
        BookReview bookReview = bookReview(10L, "채식주의자", TradeType.DELIVERY, 5.0);
        when(bookReviewRepository.findWrittenReviewsByUserId(USER_ID, null, pageable))
                .thenReturn(new PageImpl<>(List.of(bookReview), pageable, 1));

        MypageReviewResponseDTO.WrittenReviews response =
                mypageReviewService.getWrittenReviews(USER_ID, null, pageable);

        assertThat(response.totalCount()).isEqualTo(1);
        assertThat(response.content()).singleElement().satisfies(item -> {
            assertThat(item.reviewId()).isEqualTo(10L);
            assertThat(item.bookTitle()).isEqualTo("채식주의자");
            assertThat(item.exchangeType()).isEqualTo(ExchangeType.DELIVERY);
            assertThat(item.exchangeTypeLabel()).isEqualTo("택배");
            assertThat(item.reviewedAt()).isEqualTo("2026. 06. 10.");
        });
        verifyNoInteractions(memberReviewRepository);
    }

    @Test
    void receivedReviewsReturnOnlyPartnerReviewsAndExcludeReceivedBookReviews() {
        PageRequest pageable = PageRequest.of(0, 20);
        MemberReview memberReview = memberReview(20L, MemberReviewReaction.BOOM_UP, "좋았어요");
        when(memberReviewRepository.findReceivedReviewsByUserId(USER_ID, null, pageable))
                .thenReturn(new PageImpl<>(List.of(memberReview), pageable, 1));
        when(memberReviewRepository.countByTargetUserIdAndReaction(USER_ID, MemberReviewReaction.BOOM_UP, null))
                .thenReturn(38L);

        MypageReviewResponseDTO.ReceivedReviews response =
                mypageReviewService.getReceivedReviews(USER_ID, null, pageable);

        assertThat(response.positiveCount()).isEqualTo(38);
        assertThat(response.content()).singleElement().satisfies(item -> {
            assertThat(item.reviewId()).isEqualTo(20L);
            assertThat(item.reviewerId()).isEqualTo(2L);
            assertThat(item.partnerReviewType()).isEqualTo(MemberReviewReaction.BOOM_UP);
            assertThat(item.partnerReviewLabel()).isEqualTo("좋아요");
        });
        verify(bookReviewRepository, never()).findWrittenReviewsByUserId(USER_ID, null, pageable);
    }

    @Test
    void receivedPartnerReviewAllowsNullCommentAndReaction() {
        PageRequest pageable = PageRequest.of(0, 20);
        MemberReview memberReview = memberReview(21L, null, null);
        when(memberReviewRepository.findReceivedReviewsByUserId(USER_ID, null, pageable))
                .thenReturn(new PageImpl<>(List.of(memberReview), pageable, 1));
        when(memberReviewRepository.countByTargetUserIdAndReaction(USER_ID, MemberReviewReaction.BOOM_UP, null))
                .thenReturn(0L);

        MypageReviewResponseDTO.ReceivedReviewItem item =
                mypageReviewService.getReceivedReviews(USER_ID, null, pageable).content().get(0);

        assertThat(item.comment()).isNull();
        assertThat(item.partnerReviewType()).isNull();
        assertThat(item.partnerReviewLabel()).isNull();
    }

    @Test
    void reviewOrderFromLatestQueryIsPreserved() {
        PageRequest pageable = PageRequest.of(0, 20);
        BookReview latest = bookReview(12L, "최신 책", TradeType.DIRECT, 4.5);
        BookReview older = bookReview(11L, "이전 책", TradeType.DIRECT, 4.0);
        setCreatedAt(latest, LocalDateTime.of(2026, 6, 11, 9, 0));
        setCreatedAt(older, LocalDateTime.of(2026, 6, 10, 9, 0));
        when(bookReviewRepository.findWrittenReviewsByUserId(USER_ID, null, pageable))
                .thenReturn(new PageImpl<>(List.of(latest, older), pageable, 2));

        MypageReviewResponseDTO.WrittenReviews response =
                mypageReviewService.getWrittenReviews(USER_ID, null, pageable);

        assertThat(response.content())
                .extracting(MypageReviewResponseDTO.WrittenReviewItem::reviewId)
                .containsExactly(12L, 11L);
    }

    @Test
    void writtenReviewsExposePagingMetadata() {
        PageRequest pageable = PageRequest.of(1, 2);
        List<BookReview> content = List.of(
                bookReview(3L, "세 번째", TradeType.DIRECT, 3.0),
                bookReview(2L, "두 번째", TradeType.DIRECT, 2.0)
        );
        when(bookReviewRepository.findWrittenReviewsByUserId(USER_ID, null, pageable))
                .thenReturn(new PageImpl<>(content, pageable, 5));

        MypageReviewResponseDTO.WrittenReviews response =
                mypageReviewService.getWrittenReviews(USER_ID, null, pageable);

        assertThat(response.totalCount()).isEqualTo(5);
        assertThat(response.pageInfo()).isEqualTo(new MypageReviewResponseDTO.PageInfo(1, 2, 3, true));
        verify(bookReviewRepository).findWrittenReviewsByUserId(USER_ID, null, pageable);
    }

    @Test
    void receivedReviewsExposePagingMetadata() {
        PageRequest pageable = PageRequest.of(2, 1);
        MemberReview review = memberReview(1L, MemberReviewReaction.BOOM_DOWN, null);
        when(memberReviewRepository.findReceivedReviewsByUserId(USER_ID, null, pageable))
                .thenReturn(new PageImpl<>(List.of(review), pageable, 4));
        when(memberReviewRepository.countByTargetUserIdAndReaction(USER_ID, MemberReviewReaction.BOOM_UP, null))
                .thenReturn(1L);

        MypageReviewResponseDTO.ReceivedReviews response =
                mypageReviewService.getReceivedReviews(USER_ID, null, pageable);

        assertThat(response.pageInfo()).isEqualTo(new MypageReviewResponseDTO.PageInfo(2, 1, 4, true));
        assertThat(response.content().get(0).partnerReviewLabel()).isEqualTo("별로였어요");
    }

    private BookReview bookReview(Long reviewId, String title, TradeType tradeType, Double rating) {
        Groups group = Groups.builder()
                .id(100L + reviewId)
                .tradeType(tradeType)
                .build();
        User writer = user(USER_ID);
        MatchedMember matchedMember = MatchedMember.builder()
                .id(200L + reviewId)
                .group(group)
                .user(writer)
                .build();
        Book book = Book.builder()
                .id(300L + reviewId)
                .title(title)
                .author("한강")
                .build();
        MemberBook memberBook = MemberBook.builder()
                .id(400L + reviewId)
                .group(group)
                .matchedMember(matchedMember)
                .book(book)
                .build();
        BookReview review = BookReview.builder()
                .id(reviewId)
                .matchedMember(matchedMember)
                .memberBook(memberBook)
                .star(rating)
                .comment("책 후기")
                .build();
        setCreatedAt(review, LocalDateTime.of(2026, 6, 10, 9, 0));
        return review;
    }

    private MemberReview memberReview(
            Long reviewId,
            MemberReviewReaction reaction,
            String comment
    ) {
        Groups group = Groups.builder().id(500L + reviewId).build();
        MatchedMember writer = MatchedMember.builder()
                .id(600L + reviewId)
                .group(group)
                .user(user(2L))
                .build();
        MatchedMember target = MatchedMember.builder()
                .id(700L + reviewId)
                .group(group)
                .user(user(USER_ID))
                .build();
        MemberReview review = MemberReview.builder()
                .id(reviewId)
                .group(group)
                .writer(writer)
                .target(target)
                .reaction(reaction)
                .comment(comment)
                .build();
        setCreatedAt(review, LocalDateTime.of(2026, 6, 9, 9, 0));
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

    private void setCreatedAt(Object entity, LocalDateTime createdAt) {
        ReflectionTestUtils.setField(entity, "createdAt", createdAt.atZone(java.time.ZoneId.of("Asia/Seoul")).toInstant());
    }
}
