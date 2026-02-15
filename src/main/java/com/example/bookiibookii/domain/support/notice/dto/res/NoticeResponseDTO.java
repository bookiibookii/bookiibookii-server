package com.example.bookiibookii.domain.support.notice.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class NoticeResponseDTO {
    public record NoticeDetailDTO(
            Long id,
            String title,
            String content,
            String image,
            @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
            LocalDateTime createdAt
    ) {}

    public record NoticeListDTO(
            Long id,
            LocalDateTime createdAt,
            String title,
            String summary
    ) {}
}
