package com.example.bookiibookii.domain.support.report.dto.res;

import com.example.bookiibookii.domain.support.inquiry.enums.SupportStatus;
import com.example.bookiibookii.domain.support.report.enums.ReportType;

import java.time.Instant;

public class ReportResponseDTO {
    public record ReportListDTO(
            Long reportId,
            String reporterNickname,
            String groupName,
            Instant createdAt,
            ReportType reportType,
            String content,
            SupportStatus supportStatus,
            String adminReply,
            Instant resolvedAt
    ) {}

    public record AdminReportListDTO(
            Long reportId,
            String reporterNickname,
            String targetNickname,
            String groupName,
            ReportType reportType,
            SupportStatus supportStatus,
            Instant resolvedAt
    ) {}

    public record AdminReportDetailDTO(
            Long reportId,
            String reporterNickname,
            String targetNickname,
            String groupName,
            ReportType reportType,
            String content,
            Instant createdAt,
            SupportStatus supportStatus,
            String adminReply,
            String adminMemo,
            Instant resolvedAt
    ) {}
}
