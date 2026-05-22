package com.example.bookiibookii.domain.tracker.dto.res;

import java.time.LocalDate;

public record ExtendReadingPeriodResDTO(
        LocalDate newEndDate,
        Integer dDay
) {}
