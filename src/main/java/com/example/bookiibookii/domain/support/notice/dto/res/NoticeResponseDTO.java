package com.example.bookiibookii.domain.support.notice.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;

public class NoticeResponseDTO {
    public record NoticeDetailDTO(
            Long id,
            String title,
            String content,
            @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
            Instant createdAt
    ) {}

    public record NoticeListDTO(
            Long id,
            Instant createdAt,
            String title,
            String summary
    ) {}

    public record AdminNoticeListDTO(
            Long id,
            Instant createdAt,
            String title
    ) {}
}
