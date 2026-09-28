package com.example.bookiibookii.domain.tracker.resolver;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.group.util.ReadingPeriodDateCalculator;
import com.example.bookiibookii.domain.tracker.enums.TrackerDisplayStatus;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TrackerDueDateResolverTest {

    private static final LocalDate START_DATE = LocalDate.of(2026, 6, 8);

    @Test
    void readingPeriodIncludesStartDateAndEndDate() {
        assertThat(ReadingPeriodDateCalculator.endDate(START_DATE, 7))
                .isEqualTo(LocalDate.of(2026, 6, 14));
    }

    @Test
    void calculatesDDayForSevenDayReadingPeriodOnStartDate() {
        assertThat(calculate(LocalDate.of(2026, 6, 8))).isEqualTo(6);
    }

    @Test
    void calculatesDDayForSevenDayReadingPeriodOneDayBeforeEndDate() {
        assertThat(calculate(LocalDate.of(2026, 6, 13))).isEqualTo(1);
    }

    @Test
    void calculatesDDayForSevenDayReadingPeriodOnEndDateAsDZero() {
        assertThat(calculate(LocalDate.of(2026, 6, 14))).isZero();
    }

    @Test
    void clampsDDayForSevenDayReadingPeriodAfterEndDateAsDZero() {
        assertThat(calculate(LocalDate.of(2026, 6, 15))).isZero();
    }

    @Test
    void hidesDDayForShippingAndReturningStatuses() {
        TrackerDueDateResolver resolver = resolver(LocalDate.of(2026, 6, 8));
        Groups group = group(START_DATE, 7);

        assertThat(resolver.calculate(TrackerDisplayStatus.SHIPPING, group)).isNull();
        assertThat(resolver.calculate(TrackerDisplayStatus.RETURNING, group)).isNull();
    }

    private Integer calculate(LocalDate today) {
        return resolver(today).calculate(TrackerDisplayStatus.READING, group(START_DATE, 7));
    }

    private TrackerDueDateResolver resolver(LocalDate today) {
        return new TrackerDueDateResolver(Clock.fixed(
                today.atStartOfDay(ReadingPeriodDateCalculator.KST).toInstant(),
                ReadingPeriodDateCalculator.KST
        ));
    }

    private Groups group(LocalDate startDate, int readingPeriod) {
        return Groups.builder()
                .tradeType(TradeType.DELIVERY)
                .startDate(startDate)
                .readingPeriod(readingPeriod)
                .build();
    }
}
