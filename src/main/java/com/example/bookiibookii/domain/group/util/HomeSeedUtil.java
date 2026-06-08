package com.example.bookiibookii.domain.group.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class HomeSeedUtil {

    public static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter SEED_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private HomeSeedUtil() {
    }

    public static String currentSeedKey() {
        return seedKey(LocalDateTime.now(KST));
    }

    public static String seedKey(LocalDateTime kstDateTime) {
        int sixHourSlot = kstDateTime.getHour() / 6;
        return kstDateTime.format(SEED_DATE_FORMATTER) + "_" + sixHourSlot;
    }

    public static long seed(String seedKey) {
        return seedKey.hashCode();
    }

    public static int pickIndex(String seedKey, int candidateCount) {
        if (candidateCount <= 0) {
            throw new IllegalArgumentException("candidateCount must be positive");
        }
        return Math.floorMod(seed(seedKey), candidateCount);
    }

    public static String userSeedKey(Long userId, String sixHourSeedKey) {
        return userId + "_" + sixHourSeedKey;
    }

    public static LocalDateTime twentyFourHoursAgoKst() {
        return LocalDateTime.now(KST).minusHours(24);
    }
}
