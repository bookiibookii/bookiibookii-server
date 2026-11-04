package com.example.bookiibookii.domain.aladin.service;

import com.example.bookiibookii.domain.aladin.dto.BookSearchResDTO;
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

    public BookSearchResDTO searchBooks(String keyword, int page, int size) {
        SeojiClient.SeojiSearchResponse raw = seojiClient.searchBooks(keyword, page, size);

        // seoji 원본 건수: 차단 카테고리 필터 전 값으로 totalPage 계산에만 사용
        int rawTotal = parseTotalCount(raw.totalCount());

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

        int totalPage = (int) Math.ceil(rawTotal / (double) size);

        return BookSearchResDTO.builder()
                .books(books)
                .totalResults(books.size())  // 필터 후 이 페이지의 실제 결과 수
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
