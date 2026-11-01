package com.example.bookiibookii.domain.group.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class HomeSeedUtilTest {

    @Test
    void keepsSameSeedKeyWithinSixHourSlot() {
        assertThat(HomeSeedUtil.seedKey(LocalDateTime.of(2026, 6, 5, 0, 0)))
                .isEqualTo("20260605_0");
        assertThat(HomeSeedUtil.seedKey(LocalDateTime.of(2026, 6, 5, 5, 59)))
                .isEqualTo("20260605_0");
        assertThat(HomeSeedUtil.seedKey(LocalDateTime.of(2026, 6, 5, 6, 0)))
                .isEqualTo("20260605_1");
        assertThat(HomeSeedUtil.seedKey(LocalDateTime.of(2026, 6, 5, 18, 30)))
                .isEqualTo("20260605_3");
    }

    @Test
    void picksSameCandidateWithinSameBucket() {
        String bucket = HomeSeedUtil.seedKey(LocalDateTime.of(2026, 6, 5, 7, 0));

        assertThat(HomeSeedUtil.pickIndex(bucket, 15))
                .isEqualTo(HomeSeedUtil.pickIndex(bucket, 15));
    }

    @Test
    void createsUserSpecificRegionSeed() {
        String bucket = "20260605_1";

        assertThat(HomeSeedUtil.userSeedKey(1L, bucket)).isEqualTo("1_20260605_1");
        assertThat(HomeSeedUtil.userSeedKey(2L, bucket)).isEqualTo("2_20260605_1");
    }
}
