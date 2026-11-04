package com.example.bookiibookii.domain.aladin.dto;

import com.example.bookiibookii.domain.book.dto.res.BookResDTO;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class BookSearchResDTO {
    private List<BookResDTO> books;
    private int totalPage;
    private int totalResults;
}
