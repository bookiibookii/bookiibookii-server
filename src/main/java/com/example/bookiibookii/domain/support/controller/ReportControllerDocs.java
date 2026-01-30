package com.example.bookiibookii.domain.support.controller;

import com.example.bookiibookii.domain.support.dto.req.ReportRequestDTO;
import com.example.bookiibookii.domain.support.dto.res.ReportResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

public interface ReportControllerDocs {
    @Operation(
            summary = "신고하기 API",
            description = """
            신고 등록하는 API입니다.
            
            reportType
            ABUSE : 욕설/비방
            SPAM : 스팸/광고
            NO_SHOW : 책 미발송/노쇼/연락두절
            DAMAGED_BOOK : 책 파손/낙서
            """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP404_1", description = "그룹을 찾을 수 없습니다."),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "USER404_1", description = "사용자를 찾을 수 없습니다.")
    })
    ApiResponse<Void> createReport(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody @Valid ReportRequestDTO.CreateReportDTO request
    );

    @Operation(
            summary = "신고내역 조회 API",
            description = "유저의 신고내역과 관리자의 답변을 조회하는 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고내역 조회 성공")
    })
    ApiResponse<List<ReportResponseDTO.ReportListDTO>> getReportList(@AuthenticationPrincipal(expression = "user") User user);

}
