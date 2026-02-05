package com.example.bookiibookii.domain.comment.dto;

import lombok.Builder;

@Builder
public record CardWriterDto(
        Long userId,
        String name,
        String profileImage
) {}
