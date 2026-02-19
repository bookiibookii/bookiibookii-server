package com.example.bookiibookii.domain.aladin.controller;

import com.example.bookiibookii.domain.aladin.dto.AladinSearchBooksResDTO;
import com.example.bookiibookii.domain.aladin.service.AladinService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
public class AladinController {

    private final AladinService aladinBookService;

    @GetMapping("/search")
    public ApiResponse<AladinSearchBooksResDTO> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK, aladinBookService.searchBooks(keyword, page, size) );
    }

}
