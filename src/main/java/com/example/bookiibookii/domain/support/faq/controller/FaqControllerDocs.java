package com.example.bookiibookii.domain.support.faq.controller;

import com.example.bookiibookii.domain.support.faq.dto.res.FaqResponseDTO;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "FAQ", description = "FAQ 관련 API")
public interface FaqControllerDocs {

    @Operation(summary = "FAQ 목록 조회 API", description = "FAQ 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FAQ 목록 조회 성공")
    })
    ApiResponse<List<FaqResponseDTO.FaqItemDTO>> getFaqList();
}
