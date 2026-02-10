package com.example.bookiibookii.domain.support.controller;

import com.example.bookiibookii.domain.group.dto.res.GroupResponseDTO;
import com.example.bookiibookii.domain.support.dto.req.ReportRequestDTO;
import com.example.bookiibookii.domain.support.dto.res.ReportResponseDTO;
import com.example.bookiibookii.domain.user.entity.User;
import com.example.bookiibookii.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Report", description = "신고 관련 API")
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


    @Operation(summary = "신고할 그룹 조회 API (드롭다운 데이터)",
            description = "신고하기 페이지에서 유저가 속해있는 현재 진행중(MATCHED) 상태의 그룹 데이터를 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공"),
    })
    ApiResponse<List<GroupResponseDTO.GroupSummaryResponse>> getGroupSummary(
            @AuthenticationPrincipal User user
    );

    @Operation(summary = "신고할 멤버 조회 API (드롭다운 데이터)",
            description = "신고하기 페이지에서 유저가 선택한 신고 그룹에 속해있는 멤버 데이터를 조회하는 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "GROUP404_4", description = "해당 그룹의 멤버가 아닙니다.")
    })
    ApiResponse<List<GroupResponseDTO.GroupMemberResponse>> getGroupMembers(
            @AuthenticationPrincipal User user, @PathVariable(name = "groupId") Long groupId
    );
}
