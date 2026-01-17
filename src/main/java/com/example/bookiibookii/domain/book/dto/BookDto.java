package com.example.bookiibookii.domain.book.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookDto {
    private String title;
    private String author;
    //private Long price;
    private String image;
    private String publisher;
    //private LocalDate publishDate;
    private String isbn;
    private String category;   // 내부 카테고리 key
    //private Long stock;
    //private String description;
}
