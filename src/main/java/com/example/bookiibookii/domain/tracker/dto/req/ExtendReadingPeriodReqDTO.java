package com.example.bookiibookii.domain.tracker.dto.req;

import java.time.LocalDate;

public record ExtendReadingPeriodReqDTO(
        LocalDate newEndDate
) {}
