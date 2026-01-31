package com.example.bookiibookii.domain.support.controller;

import com.example.bookiibookii.domain.support.dto.req.InquiryRequestDTO;
import com.example.bookiibookii.domain.support.dto.res.InquiryResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface InquiryControllerDocs {
    @Operation(
            summary = "문의하기 API",
            description = "문의를 등록하는 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의 등록 성공")
    })
    ApiResponse<Void> createInquiry(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody @Valid InquiryRequestDTO.CreateInquiryDTO request
    );

    @Operation(
            summary = "문의내역 조회 API",
            description = "유저의 문의내역과 관리자의 답변을 조회하는 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "문의내역 조회 성공")
    })
    ApiResponse<List<InquiryResponseDTO.InquiryListDTO>> getInquiryList(@AuthenticationPrincipal(expression = "user") User user);

}
