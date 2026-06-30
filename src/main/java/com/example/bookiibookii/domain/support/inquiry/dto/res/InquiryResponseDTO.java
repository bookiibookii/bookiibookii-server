package com.example.bookiibookii.domain.support.inquiry.dto.res;

import com.example.bookiibookii.domain.support.inquiry.enums.SupportStatus;

import java.time.Instant;

public class InquiryResponseDTO {
    public record InquiryListDTO(
            Long inquiryId,
            Long userId,
            String nickname,
            Instant createdAt,
            String title,
            String content,
            SupportStatus supportStatus,
            String adminReply,
            Instant resolvedAt
    ){}
}
