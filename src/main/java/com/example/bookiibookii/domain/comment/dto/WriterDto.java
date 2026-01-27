package com.example.bookiibookii.domain.comment.dto;

import com.example.bookiibookii.domain.comment.enums.WriterRole;
import lombok.Builder;

@Builder
public record WriterDto (
        Long userId,
        String name,
        String profileImage,
        WriterRole role
){}
