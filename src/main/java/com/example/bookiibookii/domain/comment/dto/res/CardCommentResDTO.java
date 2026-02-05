package com.example.bookiibookii.domain.comment.dto.res;

import com.example.bookiibookii.domain.comment.dto.CardWriterDto;
import java.time.LocalDateTime;
import java.util.List;

public class CardCommentResDTO {

    public record Create(
            Long id
    ) {}

    public record Comment(
            Long id,
            String content,
            CardWriterDto writer,
            LocalDateTime createdAt
    ) {}

    public record ListResponse(
            long totalCount,
            List<Comment> comments
    ) {}
}
