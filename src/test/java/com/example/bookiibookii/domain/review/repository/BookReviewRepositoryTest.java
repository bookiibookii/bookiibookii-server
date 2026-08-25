package com.example.bookiibookii.domain.review.repository;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.GroupStatus;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.enums.SocialType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class BookReviewRepositoryTest {

    @Autowired
    private BookReviewRepository bookReviewRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void writtenReviewsExcludeRemovedMemberBooksFromContentAndTotalElements() {
        User user = User.builder()
                .nickName("작성자")
                .socialType(SocialType.KAKAO)
                .socialId("written-review-user")
                .build();
        entityManager.persist(user);

        Book activeBook = persistBook("9780000000101", "노출 책");
        Book removedBook = persistBook("9780000000102", "삭제 책");
        Groups group = Groups.builder()
                .book(activeBook)
                .host(user)
                .maxCapacity(2)
                .startDate(LocalDate.of(2026, 6, 1))
                .readingPeriod(14)
                .groupStatus(GroupStatus.COMPLETED)
                .tradeType(TradeType.DIRECT)
                .groupName("후기 그룹")
                .build();
        entityManager.persist(group);

        MatchedMember matchedMember = MatchedMember.builder()
                .group(group)
                .user(user)
                .build();
        entityManager.persist(matchedMember);

        MemberBook activeMemberBook = persistMemberBook(group, matchedMember, activeBook);
        MemberBook removedMemberBook = persistMemberBook(group, matchedMember, removedBook);
        removedMemberBook.markRemoved(java.time.Instant.now());

        entityManager.persist(BookReview.create(matchedMember, activeMemberBook, 5.0, "노출 후기"));
        entityManager.persist(BookReview.create(matchedMember, removedMemberBook, 4.0, "삭제 후기"));
        entityManager.flush();
        entityManager.clear();

        Page<BookReview> result = bookReviewRepository.findWrittenReviewsByUserId(
                user.getId(),
                null,
                PageRequest.of(0, 1)
        );

        assertThat(result.getContent())
                .extracting(review -> review.getMemberBook().getBook().getTitle())
                .containsExactly("노출 책");
        assertThat(result.getNumberOfElements()).isEqualTo(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void latestRatingsQueryFiltersByWriterAndFetchesRequestedBooksInLatestOrder() {
        User user = User.builder()
                .nickName("작성자")
                .socialType(SocialType.KAKAO)
                .socialId("representative-rating-user")
                .build();
        User other = User.builder()
                .nickName("다른 작성자")
                .socialType(SocialType.KAKAO)
                .socialId("other-rating-user")
                .build();
        entityManager.persist(user);
        entityManager.persist(other);

        Book book = persistBook("9780000000201", "대표 책");
        Groups firstGroup = persistGroup(book, user, "첫 그룹");
        Groups secondGroup = persistGroup(book, user, "두 번째 그룹");
        Groups otherGroup = persistGroup(book, other, "다른 사용자 그룹");
        MatchedMember firstMember = persistMatchedMember(firstGroup, user);
        MatchedMember secondMember = persistMatchedMember(secondGroup, user);
        MatchedMember otherMember = persistMatchedMember(otherGroup, other);
        MemberBook firstMemberBook = persistMemberBook(firstGroup, firstMember, book);
        MemberBook secondMemberBook = persistMemberBook(secondGroup, secondMember, book);
        MemberBook otherMemberBook = persistMemberBook(otherGroup, otherMember, book);

        entityManager.persist(BookReview.create(firstMember, firstMemberBook, 3.0, "이전 후기"));
        entityManager.flush();
        entityManager.persist(BookReview.create(secondMember, secondMemberBook, 4.5, "최신 후기"));
        entityManager.persist(BookReview.create(otherMember, otherMemberBook, 5.0, "다른 사용자 후기"));
        entityManager.flush();
        entityManager.clear();

        List<BookReview> result = bookReviewRepository.findLatestByUserIdAndBookIds(
                user.getId(),
                List.of(book.getId())
        );

        assertThat(result).extracting(BookReview::getStar).containsExactly(4.5, 3.0);
    }

    private Book persistBook(String isbn13, String title) {
        Book book = Book.builder()
                .isbn13(isbn13)
                .title(title)
                .author("저자")
                .publisher("출판사")
                .image("image")
                .totalPages(100)
                .link("link")
                .category(CustomCategory.KOREAN_NOVEL)
                .build();
        entityManager.persist(book);
        return book;
    }

    private MemberBook persistMemberBook(Groups group, MatchedMember matchedMember, Book book) {
        MemberBook memberBook = MemberBook.builder()
                .group(group)
                .matchedMember(matchedMember)
                .book(book)
                .isMine(true)
                .build();
        entityManager.persist(memberBook);
        return memberBook;
    }

    private Groups persistGroup(Book book, User host, String name) {
        Groups group = Groups.builder()
                .book(book)
                .host(host)
                .maxCapacity(2)
                .startDate(LocalDate.of(2026, 6, 1))
                .readingPeriod(14)
                .groupStatus(GroupStatus.COMPLETED)
                .tradeType(TradeType.DIRECT)
                .groupName(name)
                .build();
        entityManager.persist(group);
        return group;
    }

    private MatchedMember persistMatchedMember(Groups group, User user) {
        MatchedMember matchedMember = MatchedMember.builder()
                .group(group)
                .user(user)
                .build();
        entityManager.persist(matchedMember);
        return matchedMember;
    }
}
