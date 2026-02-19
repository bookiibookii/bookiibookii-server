package com.example.bookiibookii.domain.support.report.dto.res;

import com.example.bookiibookii.domain.support.inquiry.enums.SupportStatus;
import com.example.bookiibookii.domain.support.report.enums.ReportType;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class ReportResponseDTO {
    // 신고 내역 조회 DTO
    public record ReportListDTO(
            Long reportId,
            String reporterNickname,
            String groupName,
            @JsonFormat(pattern = "yyyy.MM.dd")
            LocalDateTime createdAt, // 신고 날짜
            ReportType reportType,
            String content,
            SupportStatus supportStatus,
            String adminReply,
            @JsonFormat(pattern = "yyyy.MM.dd")
            LocalDateTime resolvedAt
    ) {}
}
