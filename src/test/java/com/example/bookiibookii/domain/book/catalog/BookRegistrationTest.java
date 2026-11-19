package com.example.bookiibookii.domain.book.catalog;

import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.book.repository.BookRepository;
import com.example.bookiibookii.domain.book.service.BookService;
import com.example.bookiibookii.domain.book.service.LibraryCategoryMapper;
import com.example.bookiibookii.domain.book.service.BookAuthorMapper;
import com.example.bookiibookii.global.time.TimeConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
        assertThat(book.getTotalPages()).isEqualTo(311);
        assertThat(book.getLink()).isEqualTo("https://example.org/book");
        assertThat(service.getOrCreateByIsbn13(isbn).getId()).isEqualTo(book.getId());
        verify(client).naruDetail(isbn);
        verify(client).nationalDetail(isbn);
        verify(client).naruByIsbn(isbn);
        verifyNoMoreInteractions(client);
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
        return new CatalogBook(isbn, "제목", "지은이: 작가", "출판사", "https://example.org/cover", "소개",
                pages, link, "813.7", "", "03810");
    }

    private Book candidate(String isbn, int pages) {
        return Book.builder().isbn13(isbn).title("제목").author("저자").publisher("출판사")
                .image("https://example.org/old.jpg").link("https://example.org/old-book")
                .category(CustomCategory.NON_LITERATURE_ETC).totalPages(pages).build();
    }
}
