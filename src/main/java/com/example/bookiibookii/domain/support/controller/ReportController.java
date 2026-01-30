package com.example.bookiibookii.domain.support.controller;

import com.example.bookiibookii.domain.support.dto.req.ReportRequestDTO;
import com.example.bookiibookii.domain.support.dto.res.ReportResponseDTO;
import com.example.bookiibookii.domain.support.service.ReportService;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import com.example.bookiibookii.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
public class ReportController implements ReportControllerDocs {
    private final ReportService reportService;
    // 신고하기 API
    @Override
    @PostMapping("/api/report")
    public ApiResponse<Void> createReport(
            @AuthenticationPrincipal(expression = "user") User user,
            @RequestBody @Valid ReportRequestDTO.CreateReportDTO request
    ) {
        reportService.createReport(user, request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, null);
    }

    // 신고내역 조회 API
    @Override
    @GetMapping("/api/report")
    public ApiResponse<List<ReportResponseDTO.ReportListDTO>> getReportList(
            @AuthenticationPrincipal(expression = "user") User user
    ) {
        List<ReportResponseDTO.ReportListDTO> result = reportService.GetReportList(user.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND, result);
    }
}
