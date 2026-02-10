package com.example.bookiibookii.domain.book.service;

import com.example.bookiibookii.domain.aladin.config.AladinClient;
import com.example.bookiibookii.domain.book.converter.BookConverter;
import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import com.example.bookiibookii.domain.book.exception.BookException;
import com.example.bookiibookii.domain.book.exception.code.BookErrorCode;
import com.example.bookiibookii.domain.book.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookRepository bookRepository;
    private final AladinClient aladinClient;
    private final BookCategoryMapper bookCategoryMapper;
    private final BookConverter bookConverter;

    @Transactional
    public Book getOrCreateByIsbn13(String isbn13) {
        return bookRepository.findByIsbn13(isbn13)
                .orElseGet(() -> {
                    // 1) 알라딘 외부 데이터 조회
                    AladinClient.AladinBookItem item = aladinClient.lookupBookByIsbn13(isbn13);

                    // 2) 카테고리 검증 및 매핑
                    CustomCategory category = bookCategoryMapper.mapCategory(item.categoryName())
                            .orElseThrow(() -> new BookException(BookErrorCode.BLOCKED_CATEGORY));

                    // 3) 컨버터를 이용한 엔티티 생성
                    Book book = bookConverter.toEntity(item, category);

                    // 4) 저장 및 동시성 예외 처리
                    try {
                        return bookRepository.save(book);
                    } catch (DataIntegrityViolationException e) {
                        return bookRepository.findByIsbn13(isbn13)
                                .orElseThrow(() -> e);
                    }
                });
    }

}
