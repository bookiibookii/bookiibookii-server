package com.example.bookiibookii.domain.support.notice.dto.req;

import jakarta.validation.constraints.NotBlank;

public class NoticeRequestDTO {
    public record CreateNoticeDTO(
            @NotBlank(message = "제목은 필수 입력 사항입니다.")
            String title,
            @NotBlank(message = "내용은 필수 입력 사항입니다.")
            String content,
            String summary,
            String image
    ) {}

    public record UpdateNoticeDTO(
            String title,
            String content,
            String summary,
            String image
    ) {}
}
