package com.example.bookiibookii.domain.support.inquiry.controller;

import com.example.bookiibookii.domain.support.inquiry.dto.req.InquiryRequestDTO;
import com.example.bookiibookii.domain.support.inquiry.dto.res.InquiryResponseDTO;
import com.example.bookiibookii.domain.support.inquiry.service.InquiryService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
public class InquiryController implements InquiryControllerDocs{
    private final InquiryService inquiryService;
    // 문의하기 API
    @Override
    @PostMapping("/api/inquiry")
    public ApiResponse<Void> createInquiry(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody @Valid InquiryRequestDTO.CreateInquiryDTO request
    ) {
        inquiryService.createInquiry(user, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, null);
    }

    // 문의내역 조회 API
    @Override
    @GetMapping("/api/inquiry")
    public ApiResponse<List<InquiryResponseDTO.InquiryListDTO>> getInquiryList(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        List<InquiryResponseDTO.InquiryListDTO> result = inquiryService.getInquiryList(user.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }
}
