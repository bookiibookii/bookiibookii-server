package com.example.bookiibookii.domain.book.dto;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookResDTO {
    private String title;
    private String author;
    // private Long price;
    private String image;
    private String publisher;
    // private LocalDate publishDate;
    private String isbn;
    // private Long categoryId;
    private CustomCategory category;
    private String categoryLabel;
    // private Long stock;
    // private String description;
}
