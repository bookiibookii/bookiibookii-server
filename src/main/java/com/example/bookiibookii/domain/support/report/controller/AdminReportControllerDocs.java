package com.example.bookiibookii.domain.support.report.controller;

import com.example.bookiibookii.domain.support.report.dto.req.ReportRequestDTO;
import com.example.bookiibookii.domain.support.report.dto.res.ReportResponseDTO;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "Admin", description = "관리자용 API")
public interface AdminReportControllerDocs {
    @Operation(
            summary = "신고 리스트 조회 API",
            description = "관리자가 모든 사용자의 신고 내역을 조회하는 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 리스트 조회 성공"),
    })
    ApiResponse<Page<ReportResponseDTO.AdminReportListDTO>> getAllReports(
            @ParameterObject Pageable pageable
    );

    @Operation(
            summary = "신고내역 상세 조회 API",
            description = "관리자가 특정 신고내역을 상세 조회하는 API입니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고내역 상세 조회 성공")
    })
    ApiResponse<ReportResponseDTO.AdminReportDetailDTO> getReportDetail(@PathVariable(name = "reportId") Long reportId);


    @Operation(summary = "신고 처리 API",
            description = "관리자가 특정 신고내역에 대한 처리를 하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "신고 처리 성공"),
    })
    ApiResponse<Void> processReport(
            @PathVariable(name = "reportId") Long reportId,
            @RequestBody ReportRequestDTO.ProcessReportDTO request
    );
}
