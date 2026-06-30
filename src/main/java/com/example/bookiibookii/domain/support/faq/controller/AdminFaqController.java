package com.example.bookiibookii.domain.support.faq.controller;

import com.example.bookiibookii.domain.support.faq.dto.req.FaqRequestDTO;
import com.example.bookiibookii.domain.support.faq.dto.res.FaqResponseDTO;
import com.example.bookiibookii.domain.support.faq.service.AdminFaqService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/faq")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFaqController implements AdminFaqControllerDocs {

    private final AdminFaqService adminFaqService;

    @GetMapping
    public ApiResponse<List<FaqResponseDTO.FaqListDTO>> getFaqList() {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, adminFaqService.getFaqList());
    }

    @PostMapping
    public ApiResponse<Void> createFaq(@Valid @RequestBody FaqRequestDTO.CreateFaqDTO request) {
        adminFaqService.createFaq(request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, null);
    }

    @PatchMapping("/{faqId}")
    public ApiResponse<Void> updateFaq(@PathVariable Long faqId, @RequestBody FaqRequestDTO.UpdateFaqDTO request) {
        adminFaqService.updateFaq(faqId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    @DeleteMapping("/{faqId}")
    public ApiResponse<Void> deleteFaq(@PathVariable Long faqId) {
        adminFaqService.deleteFaq(faqId);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }
}
