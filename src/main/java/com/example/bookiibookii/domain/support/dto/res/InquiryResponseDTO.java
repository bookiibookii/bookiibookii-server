package com.example.bookiibookii.domain.support.dto.res;

import com.example.bookiibookii.domain.support.enums.SupportStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalDateTime;

public class InquiryResponseDTO {
    public record InquiryListDTO(
            Long inquiryId,
            String nickname,
            @JsonFormat(pattern = "yyyy.MM.dd", timezone = "Asia/Seoul")
            Instant createdAt,
            String title,
            String content,
            SupportStatus supportStatus,
            String adminReply,
            @JsonFormat(pattern = "yyyy.MM.dd")
            LocalDateTime resolvedAt
    ){}
}
