package com.example.bookiibookii.domain.support.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InquiryRequestDTO {
    // 문의 생성 요청 DTO
    public record CreateInquiryDTO(
            @NotBlank
            @Size(max = 255)
            String title,

            @NotBlank(message = "문의 내용을 입력해주세요.")
            @Size(max = 255)
            String content
    ){}
}
