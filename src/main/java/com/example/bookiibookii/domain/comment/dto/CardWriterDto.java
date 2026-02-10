package com.example.bookiibookii.domain.comment.dto;

import lombok.Builder;

@Builder
public record CardWriterDto(
        Long userId,
        String name,
        String profileImageUrl  // 프로필 이미지 Presigned GET URL
) {}
