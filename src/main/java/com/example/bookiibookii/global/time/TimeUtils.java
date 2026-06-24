package com.example.bookiibookii.global.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    private TimeUtils() {
    }

    public static LocalDate todayKst() {
        return LocalDate.now(TimeConfig.KST);
    }

    public static LocalDateTime localDateTimeKst(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, TimeConfig.KST);
    }

    public static String formatKst(Instant instant, DateTimeFormatter formatter) {
        return instant == null ? null : formatter.format(instant.atZone(TimeConfig.KST));
    }
}
