package com.example.bookiibookii.domain.comment.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CardCommentReqDTO {

    public record Create(
            @NotBlank(message = "댓글 내용을 입력해주세요.")
            @Size(max = 500, message = "댓글은 500자 이내로 입력해주세요.")
            String content
    ) {}
}
