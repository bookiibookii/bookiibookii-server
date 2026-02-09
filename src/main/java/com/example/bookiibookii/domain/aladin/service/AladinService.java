package com.example.bookiibookii.domain.aladin.service;

import com.example.bookiibookii.domain.aladin.config.AladinClient;
import com.example.bookiibookii.domain.aladin.dto.AladinSearchBooksResDTO;
import com.example.bookiibookii.domain.book.dto.BookResDTO;
import com.example.bookiibookii.domain.book.dto.TempBookResDTO;
import com.example.bookiibookii.domain.book.exception.BookException;
import com.example.bookiibookii.domain.book.exception.code.BookErrorCode;
import com.example.bookiibookii.domain.book.service.BookCategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.bookiibookii.domain.book.enums.CustomCategory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class AladinService {

    private final AladinClient aladinClient;
    private final BookCategoryMapper bookCategoryMapper;

    public AladinSearchBooksResDTO searchBooks(String keyword, int page, int size) {
        AladinClient.AladinItemSearchResponse raw = aladinClient.searchBooksByKeyword(keyword, page, size);

        int totalResults = raw.totalResults();

        List<BookResDTO> books = raw.item() == null ? List.of()
                : raw.item().stream()
                .flatMap(item -> {
                    Optional<CustomCategory> cc = bookCategoryMapper.mapCategory(item.categoryName());
                    if (cc.isEmpty()) return Stream.empty(); // 차단 카테고리일 시 제거

                    return Stream.of(
                            BookResDTO.builder()
                                    .title(nvl(item.title()))
                                    .author(nvl(item.author()))
                                    .image(nvl(item.cover()))
                                    .publisher(nvl(item.publisher()))
                                    .isbn13(nvl(item.isbn13()))
                                    .link(nvl(item.link()))
                                    .category(cc.get())
                                    .categoryLabel(cc.get().label())
                                    .build()
                    );
                })
                .toList();

        int totalPage = (int) Math.ceil(totalResults / (double) size);

        return AladinSearchBooksResDTO.builder()
                .books(books)
                .totalResults(totalResults)
                .totalPage(totalPage)
                .build();
    }

    public TempBookResDTO searchBookByISBN(String isbn13){
        AladinClient.AladinBookItem bookItem = aladinClient.lookupBookByIsbn13(isbn13);

        Optional<CustomCategory> cc = bookCategoryMapper.mapCategory(bookItem.categoryName());
        if (cc.isEmpty()) {
            throw new BookException(BookErrorCode.BLOCKED_CATEGORY);
        }

        return TempBookResDTO.builder()
                .title(nvl(bookItem.title()))
                .author(nvl(bookItem.author()))
                .image(nvl(bookItem.cover()))
                .publisher(nvl(bookItem.publisher()))
                .isbn13(nvl(bookItem.isbn13()))
                .link(nvl(bookItem.link()))
                .category(cc.get())
                .categoryLabel(cc.get().label())
                .itemPage(bookItem.itemPage())
                .build();
    }

    private String nvl(String s) {
        return (s == null) ? "" : s;
    }
}
