package com.example.bookiibookii.domain.memberbook.controller;

import com.example.bookiibookii.domain.memberbook.dto.res.PublicReadingCardResponseDTO;
import com.example.bookiibookii.domain.memberbook.exception.code.MemberBookCardSuccessCode;
import com.example.bookiibookii.domain.memberbook.service.ReadingCardShareService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/reading-cards")
@RequiredArgsConstructor
public class PublicReadingCardShareController implements PublicReadingCardShareControllerDocs {

    private final ReadingCardShareService readingCardShareService;

    @GetMapping("/{shareToken}")
    public ApiResponse<PublicReadingCardResponseDTO> getPublicReadingCard(
            @PathVariable String shareToken
    ) {
        PublicReadingCardResponseDTO response = readingCardShareService.getPublicReadingCard(shareToken);
        return ApiResponse.onSuccess(MemberBookCardSuccessCode.PUBLIC_READING_CARD_FOUND, response);
    }
}
