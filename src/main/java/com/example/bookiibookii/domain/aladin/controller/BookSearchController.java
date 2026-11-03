package com.example.bookiibookii.domain.aladin.controller;

import com.example.bookiibookii.domain.aladin.dto.BookSearchResDTO;
import com.example.bookiibookii.domain.aladin.service.BookSearchService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Book Search", description = "도서 검색 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
public class BookSearchController {

    private final BookSearchService bookSearchService;

    @GetMapping("/search")
    public ApiResponse<BookSearchResDTO> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK, bookSearchService.searchBooks(keyword, page, size));
    }

}
