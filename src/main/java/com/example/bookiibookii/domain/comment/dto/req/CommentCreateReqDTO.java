package com.example.bookiibookii.domain.comment.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(description = "부모 댓글 ID (대댓글일 때만)", example = "null", nullable = true, defaultValue = "null")
    private Long parentId;

    @Schema(description = "비밀 대댓글 여부 (대댓글일 때만 true 가능)", defaultValue = "false", example = "false")
    private boolean secret;
}
