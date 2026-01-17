package com.example.bookiibookii.domain.aladin.service;

import com.example.bookiibookii.domain.aladin.config.AladinClient;
import com.example.bookiibookii.domain.aladin.dto.AladinSearchResponseDto;
import com.example.bookiibookii.domain.book.dto.BookDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AladinService {

    private final AladinClient aladinClient;
    public AladinSearchResponseDto search(String keyword, int page, int size) {
        AladinClient.AladinSearchRawResponse raw = aladinClient.searchBooksJson(keyword, page, size);

        int totalResults = raw.totalResults();

        List<BookDto> books = raw.item() == null ? List.of()
                : raw.item().stream()
                .map(this::convertItemToBookDto)
                .toList();

        int totalPage = (int) Math.ceil(totalResults / (double) size);

        return AladinSearchResponseDto.builder()
                .books(books)
                .totalResults(totalResults)
                .totalPage(totalPage)
                .build();
    }

    private BookDto convertItemToBookDto(AladinClient.AladinBookItem item) {
        return BookDto.builder()
                .title(nvl(item.title()))
                .author(nvl(item.author()))
                // .price(item.priceStandard() == null ? 0L : item.priceStandard())
                .image(nvl(item.cover()))
                .publisher(nvl(item.publisher()))
                // .publishDate(parseDate(item.pubDate()))
                .isbn(nvl(item.isbn13()))
                .category(convertCategoryToCategoryKey(item.categoryName()))
                // .stock(-1L)
                // .description(nvl(item.description()))
                .build();
    }

    private String nvl(String s) {
        return (s == null) ? "" : s;
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            return LocalDate.parse(date, formatter);
        } catch (Exception e) {
            return null;
        }
    }

    private String convertCategoryToCategoryKey(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return "ETC";
        String[] split = categoryName.split(">");
        if (split.length < 2) return "ETC";
        return split[1].trim();
    }
}
