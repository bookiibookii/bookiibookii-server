package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class TrackerDueDateResolver {

    public Integer calculate(
            TrackerDisplayStatus displayStatus,
            Groups group
    ) {
        if (displayStatus == TrackerDisplayStatus.SHIPPING
                || displayStatus == TrackerDisplayStatus.RETURNING) {
            return null;
        }

        LocalDate dueDate = group.getStartDate() == null || group.getReadingPeriod() == null
                ? null
                : group.getStartDate().plusDays(group.getReadingPeriod());

        if (dueDate == null) {
            return null;
        }

        long days = ChronoUnit.DAYS.between(LocalDate.now(), dueDate);

        return Math.max((int) days, 0);
    }
}
