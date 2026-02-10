package com.example.bookiibookii.domain.comment.dto;

import com.example.bookiibookii.domain.comment.enums.WriterRole;
import lombok.Builder;

@Builder
public record WriterDto(
        Long userId,
        String name,
        String profileImageUrl,  // 프로필 이미지 Presigned GET URL
        WriterRole role
) {}
