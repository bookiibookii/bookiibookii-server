package com.example.bookiibookii.domain.aladin.controller;

import com.example.bookiibookii.domain.aladin.dto.AladinSearchResponseDto;
import com.example.bookiibookii.domain.aladin.service.AladinService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/test/aladin")
public class AladinController {

    private final AladinService aladinBookService;

    @GetMapping("/search")
    public AladinSearchResponseDto search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return aladinBookService.search(keyword, page, size);
    }
}
