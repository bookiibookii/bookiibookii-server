package com.example.bookiibookii.domain.support.faq.controller;

import com.example.bookiibookii.domain.support.faq.dto.req.FaqRequestDTO;
import com.example.bookiibookii.domain.support.faq.dto.res.FaqResponseDTO;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Admin", description = "관리자용 API")
public interface AdminFaqControllerDocs {

    @Operation(summary = "FAQ 목록 조회 API", description = "등록된 FAQ 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FAQ 목록 조회 성공")
    })
    ApiResponse<List<FaqResponseDTO.FaqListDTO>> getFaqList();

    @Operation(summary = "FAQ 등록 API", description = "FAQ를 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "FAQ 등록 성공")
    })
    ApiResponse<Void> createFaq(@Valid @RequestBody FaqRequestDTO.CreateFaqDTO request);

    @Operation(summary = "FAQ 수정 API", description = "FAQ를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FAQ 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FAQ를 찾을 수 없습니다.")
    })
    ApiResponse<Void> updateFaq(@PathVariable Long faqId, @RequestBody FaqRequestDTO.UpdateFaqDTO request);

    @Operation(summary = "FAQ 삭제 API", description = "FAQ를 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "FAQ 삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "FAQ를 찾을 수 없습니다.")
    })
    ApiResponse<Void> deleteFaq(@PathVariable Long faqId);
}
