package com.example.bookiibookii.domain.aladin.dto;

import com.example.bookiibookii.domain.book.dto.BookDto;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AladinSearchResponseDto {
    // json response by bookiebookie
    private List<BookDto> books;
    private int totalPage;
    private int totalResults;
}
