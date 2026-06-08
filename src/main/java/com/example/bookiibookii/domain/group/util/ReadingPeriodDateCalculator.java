package com.example.bookiibookii.domain.group.util;

import com.example.bookiibookii.domain.group.entity.Groups;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public final class ReadingPeriodDateCalculator {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private ReadingPeriodDateCalculator() {
    }

    public static LocalDate todayKst() {
        return LocalDate.now(KST);
    }

    public static LocalDate endDate(Groups group) {
        if (group == null) {
            return null;
        }
        return endDate(group.getStartDate(), group.getReadingPeriod());
    }

    public static LocalDate endDate(LocalDate startDate, Integer readingPeriod) {
        if (startDate == null || readingPeriod == null) {
            return null;
        }
        return startDate.plusDays(readingPeriod - 1L);
    }

    public static int inclusivePeriod(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate must not be null");
        }

        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }

        return Math.toIntExact(ChronoUnit.DAYS.between(startDate, endDate) + 1);
    }

    public static Integer remainingDaysUntil(LocalDate dueDate, LocalDate today) {
        if (dueDate == null || today == null) {
            return null;
        }
        return Math.max((int) ChronoUnit.DAYS.between(today, dueDate), 0);
    }
}
