package com.example.bookiibookii.domain.support.report.controller;

import com.example.bookiibookii.domain.support.report.dto.req.ReportRequestDTO;
import com.example.bookiibookii.domain.support.report.dto.res.ReportResponseDTO;
import com.example.bookiibookii.domain.support.report.service.AdminReportService;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/report")
@PreAuthorize("hasRole('ADMIN')") // 클래스 레벨에 선언하여 모든 메서드에 관리자 권한 강제
public class AdminReportController implements AdminReportControllerDocs{

    private final AdminReportService adminReportService;

    // 신고내역 조회 API
    @GetMapping
    public ApiResponse<Page<ReportResponseDTO.AdminReportListDTO>> getAllReports(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<ReportResponseDTO.AdminReportListDTO> result = adminReportService.getAllReports(pageable);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    // 신고 상세 조회
    @GetMapping("/{reportId}")
    public ApiResponse<ReportResponseDTO.AdminReportDetailDTO> getReportDetail(
            @PathVariable(name = "reportId") Long reportId
    ) {
        ReportResponseDTO.AdminReportDetailDTO result = adminReportService.getReportDetail(reportId);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    // 신고 처리 (답변 등록 및 상태 변경)
    @PostMapping("/{reportId}/process")
    public ApiResponse<Void> processReport(
            @PathVariable(name = "reportId") Long reportId,
            @RequestBody ReportRequestDTO.ProcessReportDTO request
    ) {
        adminReportService.processReport(reportId, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }
}