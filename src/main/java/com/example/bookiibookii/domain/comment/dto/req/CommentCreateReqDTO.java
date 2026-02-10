package com.example.bookiibookii.domain.comment.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CommentCreateReqDTO {
    @NotBlank(message = "댓글 내용을 입력해주세요.")
    @Size(max = 250, message = "댓글은 250자 이내로 입력해주세요.")
    private String content;

    @Positive(message = "parentId는 1 이상의 값이어야 합니다.")
    private Long parentId;
}
