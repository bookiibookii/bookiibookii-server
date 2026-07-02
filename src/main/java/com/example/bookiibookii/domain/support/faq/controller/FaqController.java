package com.example.bookiibookii.domain.support.faq.controller;

import com.example.bookiibookii.domain.support.faq.dto.res.FaqResponseDTO;
import com.example.bookiibookii.domain.support.faq.service.FaqService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/faq")
public class FaqController implements FaqControllerDocs {

    private final FaqService faqService;

    @GetMapping
    public ApiResponse<List<FaqResponseDTO.FaqItemDTO>> getFaqList() {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, faqService.getFaqList());
    }
}
