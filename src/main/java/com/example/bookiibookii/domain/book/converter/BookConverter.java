package com.example.bookiibookii.domain.book.converter;

import com.example.bookiibookii.domain.aladin.config.AladinClient;
import com.example.bookiibookii.domain.aladin.dto.res.AladinSearchBooksResDTO;
import com.example.bookiibookii.domain.book.dto.res.BookResDTO;
import com.example.bookiibookii.domain.book.dto.res.TempBookResDTO;
import com.example.bookiibookii.domain.book.entity.Book;
import com.example.bookiibookii.domain.book.enums.CustomCategory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookConverter {
    /**
     * 알라딘 아이템 -> Book 엔티티 변환
     */
    public Book toEntity(AladinClient.AladinBookItem item, CustomCategory category) {
        return Book.builder()
                .isbn13(item.isbn13())
                .title(item.title())
                .author(item.author())
                .publisher(item.publisher())
                .image(item.cover())
                .totalPages(item.itemPage())
                .link(item.link())
                .category(category)
                .build();
    }
}
