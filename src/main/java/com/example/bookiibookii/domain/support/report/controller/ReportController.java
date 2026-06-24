package com.example.bookiibookii.domain.support.report.controller;

import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.group.service.GroupService;
import com.example.bookiibookii.domain.support.report.dto.req.ReportRequestDTO;
import com.example.bookiibookii.domain.support.report.dto.res.ReportResponseDTO;
import com.example.bookiibookii.domain.support.report.service.ReportService;
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
    private final GroupService groupService;
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
        List<ReportResponseDTO.ReportListDTO> result = reportService.getReportList(user.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }


    // 신고할 그룹 조회 API
    @Override
    @GetMapping("/api/report/groups/my")
    public ApiResponse<List<GroupResponseDTO.GroupSummaryResponse>> getGroupSummary(
            @AuthenticationPrincipal(expression = "user") User user) {
        List<GroupResponseDTO.GroupSummaryResponse> result = groupService.getGroupSummary(user.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

    // 신고할 그룹멤버 조회 API
    @Override
    @GetMapping("/api/report/{groupId}/members")
    public ApiResponse<List<GroupResponseDTO.GroupMemberResponse>> getGroupMembers(
            @AuthenticationPrincipal(expression = "user") User user,
            @PathVariable(name = "groupId") Long groupId) {
        List<GroupResponseDTO.GroupMemberResponse> result = groupService.getGroupMembers(groupId, user.getId());
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result);
    }

}
