package com.example.bookiibookii.domain.support.notice.dto.req;

import jakarta.validation.constraints.NotNull;

public class NoticeRequestDTO {
    public record CreateNoticeDTO(
            @NotNull
            String title,
            @NotNull
            String content,
            String summary,
            String image
    ){}
}
