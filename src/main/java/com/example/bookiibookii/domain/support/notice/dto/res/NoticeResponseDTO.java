package com.example.bookiibookii.domain.support.notice.dto.res;

import java.time.Instant;

public class NoticeResponseDTO {
    public record AdminNoticeDetailDTO(
            Long noticeId,
            String title,
            String summary,
            String content,
            String authorNickname,
            String updatedByNickname,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record NoticeDetailDTO(
            Long id,
            String title,
            String summary,
            String content,
            String authorNickname,
            Instant createdAt,
            Instant updatedAt
    ) {}

    public record NoticeListDTO(
            Long id,
            Instant createdAt,
            String title,
            String summary,
            boolean isRead
    ) {}

    public record AdminNoticeListDTO(
            Long id,
            String title,
            String summary,
            String authorNickname,
            String updatedByNickname,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
