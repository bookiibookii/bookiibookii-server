package com.example.bookiibookii.domain.support.dto.res;

import com.example.bookiibookii.domain.support.enums.ReportStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class InquiryResponseDTO {
    public record InquiryListDTO(
            Long inquiryId,
            String nickname,
            @JsonFormat(pattern = "yyyy.MM.dd")
            LocalDateTime createdAt,
            String title,
            String content,
            ReportStatus reportStatus,
            String adminReply,
            @JsonFormat(pattern = "yyyy.MM.dd")
            LocalDateTime resolvedAt
    ){}
}
