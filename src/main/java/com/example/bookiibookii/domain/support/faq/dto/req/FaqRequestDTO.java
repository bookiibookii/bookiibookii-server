package com.example.bookiibookii.domain.support.faq.dto.req;

import jakarta.validation.constraints.NotBlank;

public class FaqRequestDTO {

    public record CreateFaqDTO(
            @NotBlank(message = "질문은 필수 입력 사항입니다.")
            String question,
            @NotBlank(message = "답변은 필수 입력 사항입니다.")
            String answer
    ) {}

    public record UpdateFaqDTO(
            String question,
            String answer
    ) {}
}
