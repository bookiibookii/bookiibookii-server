package com.example.bookiibookii.domain.tracker.dto;

import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import lombok.Builder;

@Builder
public record TrackerStepInfo(
        ReadingStatus status,
        String title,
        String description,
        boolean completed
) {}
