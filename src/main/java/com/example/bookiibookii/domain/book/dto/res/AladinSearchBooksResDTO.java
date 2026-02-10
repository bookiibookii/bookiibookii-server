package com.example.bookiibookii.domain.book.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AladinSearchBooksResDTO {
    // json response by bookiebookie
    private List<BookResDTO> books;
    private int totalPage;
    private int totalResults;
}
