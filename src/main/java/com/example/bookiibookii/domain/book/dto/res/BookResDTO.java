package com.example.bookiibookii.domain.book.dto.res;

import com.example.bookiibookii.domain.book.enums.CustomCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BookResDTO {
    private String title;
    private String author;
    private String image;
    private String publisher;
    private String isbn13;
    private String link;
    private CustomCategory category;
    private String categoryLabel;
}
