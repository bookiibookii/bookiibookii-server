package com.example.bookiibookii.domain.support.dto.res;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.Instant;
import java.time.LocalDateTime;

public class NoticeResponseDTO {
    public record NoticeDetailDTO(
            Long id,
            String title,
            String content,
            String image,
            @JsonFormat(pattern = "yyyy.MM.dd HH:mm", timezone = "Asia/Seoul")
            Instant createdAt
    ) {}

    public record NoticeListDTO(
            Long id,
            Instant createdAt,
            String title,
            String summary
    ) {}
}
