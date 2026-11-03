package com.example.bookiibookii.domain.aladin.service;

import com.example.bookiibookii.domain.aladin.dto.AladinSearchBooksResDTO;
import com.example.bookiibookii.domain.book.dto.res.BookResDTO;
import com.example.bookiibookii.domain.book.service.BookAuthorMapper;
import com.example.bookiibookii.domain.book.service.BookCategoryMapper;
import com.example.bookiibookii.domain.nlgoKr.config.SeojiClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.bookiibookii.domain.book.enums.CustomCategory;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BookSearchService {

    private final SeojiClient seojiClient;
    private final BookCategoryMapper bookCategoryMapper;
    private final BookAuthorMapper bookAuthorMapper;

    public AladinSearchBooksResDTO searchBooks(String keyword, int page, int size) {
        SeojiClient.SeojiSearchResponse raw = seojiClient.searchBooks(keyword, page, size);

        int totalResults = parseTotalCount(raw.totalCount());

        List<BookResDTO> books = raw.docs() == null ? List.of()
                : raw.docs().stream()
                .flatMap(item -> {
                    Optional<CustomCategory> cc = bookCategoryMapper.mapCategoryByKdc(
                            item.kdc(),
                            item.addCode(),
                            item.isbn13(),
                            item.title()
                    );
                    if (cc.isEmpty()) return Stream.empty();

                    return Stream.of(
                            BookResDTO.builder()
                                    .title(nvl(item.title()))
                                    .author(nvl(bookAuthorMapper.mapFirstWriterOnly(item.author())))
                                    .image(nvl(item.titleUrl()))
                                    .publisher(nvl(item.publisher()))
                                    .isbn13(nvl(item.isbn13()))
                                    .link("https://seoji.nl.go.kr/landingPage?isbn=" + nvl(item.isbn13()))
                                    .category(cc.get())
                                    .categoryLabel(cc.get().getLabel())
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

    private int parseTotalCount(String totalCount) {
        if (totalCount == null || totalCount.isBlank()) return 0;
        try {
            return Integer.parseInt(totalCount.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String nvl(String s) {
        return (s == null) ? "" : s;
    }
}
