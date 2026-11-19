package com.example.bookiibookii.domain.book.service;

import com.example.bookiibookii.domain.book.catalog.HybridBookCatalog;
import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final HybridBookCatalog catalog;
    private final Clock clock;

    @Transactional
    public Book getOrCreateByIsbn13(String isbn13) {
        return bookRepository.findByIsbn13(isbn13).orElseGet(() -> {
            Book candidate = catalog.lookup(isbn13);
            bookRepository.insertIfAbsent(candidate, candidate.getCategory().name(), clock.instant());
            // A locking read sees the winning insert even under MySQL REPEATABLE READ.
            return bookRepository.findCurrentByIsbn13(isbn13).orElseThrow();
        });
    }
}
