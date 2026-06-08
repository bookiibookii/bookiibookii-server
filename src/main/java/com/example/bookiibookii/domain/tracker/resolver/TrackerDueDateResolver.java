package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.util.ReadingPeriodDateCalculator;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

@Component
public class TrackerDueDateResolver {

    private final Clock clock;

    public TrackerDueDateResolver() {
        this(Clock.system(ReadingPeriodDateCalculator.KST));
    }

    public TrackerDueDateResolver(Clock clock) {
        this.clock = clock;
    }

    public Integer calculate(
            TrackerDisplayStatus displayStatus,
            Groups group
    ) {
        if (displayStatus == TrackerDisplayStatus.SHIPPING
                || displayStatus == TrackerDisplayStatus.RETURNING) {
            return null;
        }

        LocalDate dueDate = ReadingPeriodDateCalculator.endDate(group);
        if (dueDate == null) {
            return null;
        }

        return ReadingPeriodDateCalculator.remainingDaysUntil(dueDate, LocalDate.now(clock));
    }
}
