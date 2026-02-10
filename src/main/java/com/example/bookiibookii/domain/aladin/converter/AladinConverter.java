package com.example.bookiibookii.domain.aladin.converter;

import com.example.bookiibookii.domain.aladin.config.AladinClient;
import com.example.bookiibookii.domain.aladin.dto.res.AladinSearchBooksResDTO;
import com.example.bookiibookii.domain.book.dto.res.BookResDTO;
import com.example.bookiibookii.domain.book.dto.res.TempBookResDTO;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AladinConverter {
    /**
     * 알라딘 개별 아이템 -> 내부 BookResDTO 변환
     */
    public BookResDTO toBookResDTO(AladinClient.AladinBookItem item, CustomCategory category) {
        return BookResDTO.builder()
                .title(nvl(item.title()))
                .author(nvl(item.author()))
                .image(nvl(item.cover()))
                .publisher(nvl(item.publisher()))
                .isbn13(nvl(item.isbn13()))
                .link(nvl(item.link()))
                .category(category)
                .categoryLabel(category.label())
                .build();
    }

    /**
     * 알라딘 검색 결과 리스트 변환
     */
    public AladinSearchBooksResDTO toSearchBooksResDTO(List<BookResDTO> books, int totalResults, int size) {
        int totalPage = (int) Math.ceil(totalResults / (double) size);

        return AladinSearchBooksResDTO.builder()
                .books(books)
                .totalResults(totalResults)
                .totalPage(totalPage)
                .build();
    }

    /**
     * 알라딘 개별 아이템 -> TempBookResDTO 변환 (상세조회용)
     */
    public TempBookResDTO toTempBookResDTO(AladinClient.AladinBookItem item, CustomCategory category) {
        return TempBookResDTO.builder()
                .title(nvl(item.title()))
                .author(nvl(item.author()))
                .image(nvl(item.cover()))
                .publisher(nvl(item.publisher()))
                .isbn13(nvl(item.isbn13()))
                .link(nvl(item.link()))
                .category(category)
                .categoryLabel(category.label())
                .itemPage(item.itemPage())
                .build();
    }

    private String nvl(String s) {
        return (s == null) ? "" : s;
    }
}
