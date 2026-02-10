package com.example.bookiibookii.domain.aladin.service;

import com.example.bookiibookii.domain.aladin.config.AladinClient;
import com.example.bookiibookii.domain.aladin.converter.AladinConverter;
import com.example.bookiibookii.domain.aladin.dto.res.AladinSearchBooksResDTO;
import com.example.bookiibookii.domain.book.dto.res.BookResDTO;
import com.example.bookiibookii.domain.book.dto.res.TempBookResDTO;
import com.example.bookiibookii.domain.book.exception.BookException;
import com.example.bookiibookii.domain.book.exception.code.BookErrorCode;
import com.example.bookiibookii.domain.book.service.BookCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.bookiibookii.domain.book.enums.CustomCategory;

import java.util.List;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AladinService {

    private final AladinClient aladinClient;
    private final BookCategoryMapper bookCategoryMapper;
    private final AladinConverter aladinConverter;

    public AladinSearchBooksResDTO searchBooks(String keyword, int page, int size) {
        AladinClient.AladinItemSearchResponse raw = aladinClient.searchBooksByKeyword(keyword, page, size);

        List<BookResDTO> books = raw.item() == null ? List.of()
                : raw.item().stream()
                .flatMap(item -> bookCategoryMapper.mapCategory(item.categoryName())
                        .map(category -> Stream.of(aladinConverter.toBookResDTO(item, category)))
                        .orElseGet(Stream::empty))
                .toList();

        return aladinConverter.toSearchBooksResDTO(books, raw.totalResults(), size);
    }

    public TempBookResDTO searchBookByISBN(String isbn13){
        AladinClient.AladinBookItem bookItem = aladinClient.lookupBookByIsbn13(isbn13);

        CustomCategory cc = bookCategoryMapper.mapCategory(bookItem.categoryName())
                .orElseThrow(() -> new BookException(BookErrorCode.BLOCKED_CATEGORY));

        return aladinConverter.toTempBookResDTO(bookItem, cc);
    }
}
