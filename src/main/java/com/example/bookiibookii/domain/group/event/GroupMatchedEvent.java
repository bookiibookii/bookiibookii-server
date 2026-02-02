package com.example.bookiibookii.domain.group.event;

import java.time.LocalDate;

public record GroupMatchedEvent(
        Long groupId,
        Long hostId,
        LocalDate startDate,
        int maxCapacity
) {}
