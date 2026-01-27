package com.example.bookiibookii.domain.comment.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentCreateReqDTO {
    @NotBlank
    private String content;

    @Positive(message = "parentId는 1 이상의 값이어야 합니다.")
    private Long parentId;
}
