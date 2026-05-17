package com.example.bookiibookii.domain.tracker.dto.res;

import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import lombok.Builder;

@Builder
public record ReadingProgressResponseDTO(
        Long memberBookId,
        Integer currentPage,
        Integer totalPages,
        Integer progressRate,
        ReadingStatus readingStatus,
        String readingStatusText,
        Integer dDay
) {}
