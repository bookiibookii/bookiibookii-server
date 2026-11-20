package com.example.bookiibookii.domain.book.catalog;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.book.repository.BookRepository;
import com.example.bookiibookii.domain.book.service.BookService;
import com.example.bookiibookii.domain.book.service.LibraryCategoryMapper;
import com.example.bookiibookii.domain.book.service.BookAuthorMapper;
import com.example.bookiibookii.domain.group.entity.Application;
import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.group.enums.ApplicationStatus;
import com.example.bookiibookii.domain.memberbook.entity.MemberBook;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.domain.user.entity.UserBook;
import com.example.bookiibookii.domain.user.enums.SocialType;
import com.example.bookiibookii.global.time.TimeConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DataJpaTest(showSql = false)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({BookService.class, HybridBookCatalog.class, LibraryCategoryMapper.class, BookAuthorMapper.class, TimeConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class BookRegistrationTest {
    @Autowired EntityManager em;
    @Autowired BookService service;
    @Autowired BookRepository books;
    @MockitoBean LibraryCatalogClient client;

    @Test
    void existingEditionIsReturnedWithItsOriginalIdAndMetadata() {
        var savedId = books.saveAndFlush(candidate("9788936434267", 210)).getId();
        var saved = books.findById(savedId).orElseThrow();
        Book result = service.getOrCreateByIsbn13(saved.getIsbn13());
        assertThat(result).usingRecursiveComparison().isEqualTo(saved);
        verifyNoInteractions(client);
    }

    @Test
    void savesNewEditionWithProviderFieldsThenReusesIt() {
        String isbn = "9791127298593";
        when(client.naruDetail(isbn)).thenReturn(List.of(metadata(isbn, null, "")));
        when(client.nationalDetail(isbn)).thenReturn(List.of(metadata(isbn, 311, "")));
        when(client.naruByIsbn(isbn)).thenReturn(List.of(metadata(isbn, null, "https://example.org/book")));
        Book book = service.getOrCreateByIsbn13(isbn);
        assertThat(books.findById(book.getId()).orElseThrow().getAuthor()).isEqualTo("Austen, Jane");
        assertThat(book.getTotalPages()).isEqualTo(311);
        assertThat(book.getLink()).isEqualTo("https://example.org/book");
        assertThat(service.getOrCreateByIsbn13(isbn).getId()).isEqualTo(book.getId());
        verify(client).naruDetail(isbn);
        verify(client).nationalDetail(isbn);
        verify(client).naruByIsbn(isbn);
        verifyNoMoreInteractions(client);
    }

    @Test
    @Transactional
    void linklessEditionRetainsShelfApplicationAndReadingReferencesAfterExchange() {
        String isbn = "9780306406157";
        when(client.nationalDetail(isbn)).thenReturn(List.of(metadata(isbn, 240, "")));
        Book guestBook = service.getOrCreateByIsbn13(isbn);
        Book hostBook = books.save(candidate("9780140327243", 180));
        var host = User.builder().socialType(SocialType.KAKAO).socialId("catalog-host").nickName("방장").build();
        var guest = User.builder().socialType(SocialType.KAKAO).socialId("catalog-guest").nickName("신청자").build();
        em.persist(host);
        em.persist(guest);
        var shelf = UserBook.createFavorite(guest, guestBook, 1);
        em.persist(shelf);
        var group = Groups.builder().book(hostBook).host(host).build();
        em.persist(group);
        var application = Application.builder().book(guestBook).group(group).guest(guest)
                .applicationStatus(ApplicationStatus.ACCEPTED).build();
        em.persist(application);
        var member = MatchedMember.builder().group(group).user(host).build();
        em.persist(member);
        var own = MemberBook.builder().group(group).matchedMember(member).book(hostBook).isMine(true).build();
        var partner = MemberBook.builder().group(group).matchedMember(member).book(guestBook).isMine(false).build();
        em.persist(own);
        em.persist(partner);
        member.startMatchedReading(own, Instant.now());
        member.updateReadingStatus(ReadingStatus.EXCHANGING);
        member.changeCurrentBook(partner, Instant.now());
        partner.updateCurrentPage(12);
        em.flush();
        em.clear();

        Book reloaded = service.getOrCreateByIsbn13(isbn);
        assertThat(reloaded.getId()).isEqualTo(guestBook.getId());
        assertThat(reloaded.getLink()).isEmpty();
        assertThat(em.find(UserBook.class, shelf.getId()).getBook()).isEqualTo(reloaded);
        assertThat(em.find(Application.class, application.getApplicationId()).getBook()).isEqualTo(reloaded);
        assertThat(em.find(Groups.class, group.getId()).getBook().getId()).isEqualTo(hostBook.getId());
        var reading = em.find(MatchedMember.class, member.getId()).getCurrentMemberBook();
        assertThat(reading.getBook()).isEqualTo(reloaded);
        assertThat(reading.getCurrentPage()).isEqualTo(12);
    }

    @Test
    void simultaneousMissesReturnOneUnmodifiedWinningRow() throws Exception {
        String isbn = "9788936434120";
        var bothMissed = new CyclicBarrier(2);
        var sequence = new AtomicInteger();
        when(client.naruDetail(isbn)).thenReturn(List.of(metadata(isbn, null, "")));
        when(client.naruByIsbn(isbn)).thenReturn(List.of(metadata(isbn, null, "https://example.org/book")));
        when(client.nationalDetail(isbn)).thenAnswer(call -> {
            int pages = sequence.incrementAndGet();
            bothMissed.await(10, TimeUnit.SECONDS);
            return List.of(metadata(isbn, pages, ""));
        });
        var threads = Executors.newFixedThreadPool(2);
        try {
            var a = threads.submit(() -> service.getOrCreateByIsbn13(isbn));
            var b = threads.submit(() -> service.getOrCreateByIsbn13(isbn));
            Book first = a.get(20, TimeUnit.SECONDS);
            Book second = b.get(20, TimeUnit.SECONDS);
            assertThat(first.getId()).isEqualTo(second.getId());
            assertThat(first.getTotalPages()).isEqualTo(second.getTotalPages());
            assertThat(books.findByIsbn13(isbn).orElseThrow().getTotalPages()).isEqualTo(first.getTotalPages());
        } finally {
            threads.shutdownNow();
        }
    }

    private CatalogBook metadata(String isbn, Integer pages, String link) {
        return new CatalogBook(isbn, "제목", "지은이: Austen, Jane；김철수", "출판사", "https://example.org/cover", "소개",
                pages, link, "813.7", "", "", "03810");
    }

    private Book candidate(String isbn, int pages) {
        return Book.builder().isbn13(isbn).title("제목").author("저자").publisher("출판사")
                .image("https://example.org/old.jpg").link("https://example.org/old-book")
                .category(CustomCategory.NON_LITERATURE_ETC).totalPages(pages).build();
    }
}
