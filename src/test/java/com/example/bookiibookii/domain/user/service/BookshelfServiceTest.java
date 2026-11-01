package com.example.bookiibookii.domain.user.service;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.service.BookService;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.repository.MatchedMemberRepository;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.memberbook.repository.MemberBookRepository;
import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.review.repository.BookReviewRepository;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserBook;
import com.example.bookiibookii.domain.user.repository.UserBookRepository;
import com.example.bookiibookii.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookshelfServiceTest {

    @Mock private UserBookRepository userBookRepository;
    @Mock private MemberBookRepository memberBookRepository;
    @Mock private BookReviewRepository bookReviewRepository;
    @Mock private MatchedMemberRepository matchedMemberRepository;
    @Mock private UserRepository userRepository;
    @Mock private BookService bookService;

    @InjectMocks
    private BookshelfService bookshelfService;

    @Test
    void representativeBooksUseLatestUserReviewRatingWithSingleBatchQuery() {
        User user = User.builder().id(1L).build();
        Book reviewedBook = Book.builder().id(10L).title("평가한 책").build();
        Book unratedBook = Book.builder().id(20L).title("평가 없는 책").build();
        UserBook first = UserBook.builder()
                .id(100L)
                .user(user)
                .book(reviewedBook)
                .displayOrder(1)
                .build();
        UserBook second = UserBook.builder()
                .id(200L)
                .user(user)
                .book(unratedBook)
                .displayOrder(2)
                .build();
        MatchedMember reviewer = MatchedMember.builder().id(30L).user(user).build();
        MemberBook memberBook = MemberBook.builder()
                .id(40L)
                .book(reviewedBook)
                .matchedMember(reviewer)
                .build();
        BookReview latestReview = BookReview.builder()
                .id(50L)
                .matchedMember(reviewer)
                .memberBook(memberBook)
                .star(4.5)
                .build();

        when(userBookRepository.findRepresentativeBooks(user.getId())).thenReturn(List.of(first, second));
        when(bookReviewRepository.findLatestByUserIdAndBookIds(user.getId(), List.of(10L, 20L)))
                .thenReturn(List.of(latestReview));

        var response = bookshelfService.getBookshelf(user.getId());

        assertThat(response.representativeBooks())
                .extracting(item -> item.rating())
                .containsExactly(4.5, null);
        verify(bookReviewRepository).findLatestByUserIdAndBookIds(user.getId(), List.of(10L, 20L));
    }
}
