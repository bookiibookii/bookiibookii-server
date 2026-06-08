package com.example.bookiibookii.domain.tracker.dto;

import com.example.bookiibookii.domain.tracker.enums.TrackerStepStatus;
import lombok.Builder;

@Builder
public record TrackerStepInfo(
        TrackerStepStatus status,
        String title,
        String description,
        boolean completed
) {}
