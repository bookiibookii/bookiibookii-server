package com.example.bookiibookii.domain.aladin.service;

import com.example.bookiibookii.domain.aladin.config.AladinClient;
import com.example.bookiibookii.domain.aladin.dto.AladinSearchBooksResDTO;
import com.example.bookiibookii.domain.book.dto.res.BookResDTO;
import com.example.bookiibookii.domain.book.service.BookAuthorMapper;
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
    private final BookAuthorMapper bookAuthorMapper;

    public AladinSearchBooksResDTO searchBooks(String keyword, int page, int size) {
        AladinClient.AladinItemSearchResponse raw = aladinClient.searchBooksByKeyword(keyword, page, size);

        int totalResults = raw.totalResults();

        List<BookResDTO> books = raw.item() == null ? List.of()
                : raw.item().stream()
                .flatMap(item -> {
                    Optional<CustomCategory> cc = bookCategoryMapper.mapCategory(
                            item.categoryId(),
                            item.categoryName(),
                            item.isbn13(),
                            item.title()
                    );
                    if (cc.isEmpty()) return Stream.empty(); // 차단 카테고리일 시 제거

                    return Stream.of(
                            BookResDTO.builder()
                                    .title(nvl(item.title()))
                                    .author(nvl(bookAuthorMapper.mapFirstWriterOnly(item.author())))
                                    .image(nvl(item.cover()))
                                    .publisher(nvl(item.publisher()))
                                    .isbn13(nvl(item.isbn13()))
                                    .link(nvl(item.link()))
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

    private String nvl(String s) {
        return (s == null) ? "" : s;
    }
}
