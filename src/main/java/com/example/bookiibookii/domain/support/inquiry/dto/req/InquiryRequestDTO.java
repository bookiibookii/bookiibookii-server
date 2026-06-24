package com.example.bookiibookii.domain.support.inquiry.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InquiryRequestDTO {

    // [유저] 문의 생성 요청 DTO
    public record CreateInquiryDTO(
            @NotBlank(message = "제목을 입력해주세요.")
            @Size(max = 255)
            String title,

            @NotBlank(message = "문의 내용을 입력해주세요.")
            @Size(max = 255)
            String content
    ){}

    // [관리자] 문의 답변 등록/수정 요청 DTO
    public record AnswerInquiryDTO(
            @NotBlank(message = "답변 내용을 입력해주세요.")
            @Size(max = 1000)
            String adminReply
    ){}
}