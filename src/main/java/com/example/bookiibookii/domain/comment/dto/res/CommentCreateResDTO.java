package com.example.bookiibookii.domain.comment.dto.res;

import com.example.bookiibookii.domain.comment.dto.WriterDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentCreateResDTO {
    private Long commentId;
    private Long groupId;
    private Long parentId;
    private String content;
    private LocalDateTime createdAt;
    private WriterDto writer;
}
