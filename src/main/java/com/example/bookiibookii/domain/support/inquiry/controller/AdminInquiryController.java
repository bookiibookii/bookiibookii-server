package com.example.bookiibookii.domain.support.inquiry.controller;

import com.example.bookiibookii.domain.support.inquiry.dto.req.InquiryRequestDTO;
import com.example.bookiibookii.domain.support.inquiry.dto.res.InquiryResponseDTO;
import com.example.bookiibookii.domain.support.inquiry.service.AdminInquiryService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/inquiry")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInquiryController implements AdminInquiryControllerDocs {

    private final AdminInquiryService adminInquiryService;

    // 모든 문의 리스트 조회
    @Override
    @GetMapping
    public ApiResponse<Page<InquiryResponseDTO.InquiryListDTO>> getAllInquiries(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        Page<InquiryResponseDTO.InquiryListDTO> result = adminInquiryService.getAllInquiries(pageable);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    // 특정 문의 상세 조회
    @Override
    @GetMapping("/{inquiryId}")
    public ApiResponse<InquiryResponseDTO.InquiryListDTO> getInquiryDetail(@PathVariable Long inquiryId) {
        InquiryResponseDTO.InquiryListDTO result = adminInquiryService.getInquiryDetail(inquiryId);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    // 문의 답변 등록 및 수정
    @Override
    @PatchMapping("/{inquiryId}/answer")
    public ApiResponse<Void> answerInquiry(
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryRequestDTO.AnswerInquiryDTO request
    ) {
        adminInquiryService.answerInquiry(inquiryId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }
}