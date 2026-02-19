package com.example.bookiibookii.domain.support.inquiry.controller;

import com.example.bookiibookii.domain.support.inquiry.dto.req.InquiryRequestDTO;
import com.example.bookiibookii.domain.support.inquiry.dto.res.InquiryResponseDTO;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Admin Inquiry", description = "관리자용 문의 관리 API")
public interface AdminInquiryControllerDocs {

    @Operation(summary = "전체 문의 내역 조회 API", description = "관리자가 모든 사용자의 문의 내역을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ApiResponse<List<InquiryResponseDTO.InquiryListDTO>> getAllInquiries();

    @Operation(summary = "문의 상세 조회 API", description = "특정 문의의 상세 내용을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    ApiResponse<InquiryResponseDTO.InquiryListDTO> getInquiryDetail(@PathVariable Long inquiryId);

    @Operation(summary = "문의 답변 등록/수정 API", description = "특정 문의에 대해 답변을 작성하거나 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "답변 처리 성공")
    })
    ApiResponse<Void> answerInquiry(
            @PathVariable Long inquiryId,
            @Valid @RequestBody InquiryRequestDTO.AnswerInquiryDTO request
    );
}