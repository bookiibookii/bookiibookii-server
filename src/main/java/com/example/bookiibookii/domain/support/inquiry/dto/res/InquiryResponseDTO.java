package com.example.bookiibookii.domain.support.inquiry.dto.res;

import com.example.bookiibookii.domain.support.inquiry.enums.SupportStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.Instant;

public class InquiryResponseDTO {
    public record InquiryListDTO(
            Long inquiryId,
            Long userId,
            String nickname,
            @JsonFormat(pattern = "yyyy.MM.dd")
            Instant createdAt,
            String title,
            String content,
            SupportStatus supportStatus,
            String adminReply,
            @JsonFormat(pattern = "yyyy.MM.dd")
            Instant resolvedAt
    ){}



}
