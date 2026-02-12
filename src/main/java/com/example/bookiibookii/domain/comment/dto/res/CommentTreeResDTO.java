package com.example.bookiibookii.domain.comment.dto.res;

import com.example.bookiibookii.domain.comment.dto.WriterDto;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
public class CommentTreeResDTO {
    private Long id;
    private boolean deleted;
    private boolean secret;
    private String content;
    private Long parentId;
    private WriterDto writer;
    private LocalDateTime createdAt;

    @Builder.Default
    private List<CommentTreeResDTO> children = new ArrayList<>();

    public void addChild(CommentTreeResDTO child) {
        this.children.add(child);
    }
}
