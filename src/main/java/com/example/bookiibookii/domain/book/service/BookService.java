package com.example.bookiibookii.domain.book.service;

import com.example.bookiibookii.domain.aladin.config.AladinClient;
import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.book.exception.BookException;
import com.example.bookiibookii.domain.book.exception.code.BookErrorCode;
import com.example.bookiibookii.domain.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final AladinClient aladinClient;
    private final BookCategoryMapper bookCategoryMapper;
    private final BookAuthorMapper bookAuthorMapper;

    // Transactional 수정 필요 (save에만 걸리게끔)
    @Transactional
    public Book getOrCreateByIsbn13(String isbn13) {
        // 그룹 생성 시 book 저장 로직

        return bookRepository.findByIsbn13(isbn13) // db에 저장된 책 있을 시 - 그걸로 return
                .orElseGet(() -> { // 없을 시
                    // 1) 알라딘에서 단건 조회
                    AladinClient.AladinBookItem item = aladinClient.lookupBookByIsbn13(isbn13);
                    Optional<CustomCategory> cc = bookCategoryMapper.mapCategory(item.categoryName());
                    if (cc.isEmpty()) {
                        throw new BookException(BookErrorCode.BLOCKED_CATEGORY);
                    }

                    // 2) 엔티티 생성
                    Book book = Book.builder()
                            .isbn13(item.isbn13())
                            .title(item.title())
                            .author(bookAuthorMapper.mapFirstWriterOnly(item.author()))
                            .publisher(item.publisher())
                            .image(item.cover())
                            .totalPages(item.itemPage())
                            .link(item.link())
                            .category(cc.get())
                            .build();

                    // 3) 저장
                    try {
                        return bookRepository.save(book);
                    } catch (DataIntegrityViolationException e) {
                        // 동시에 같은 isbn13 저장 시도 → unique 충돌 → 재조회
                        return bookRepository.findByIsbn13(isbn13)
                                .orElseThrow(() -> e);
                    }
                });
    }

}
