package com.example.bookiibookii.domain.group.entity;

import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeetingTest {

    @Test
    void rejectsCreateWithoutCoordinates() {
        assertThatThrownBy(() -> Meeting.create(
                Groups.builder().build(),
                MatchedMember.builder().build(),
                ExchangeRound.FIRST_EXCHANGE,
                "강남역",
                "서울특별시 강남구 강남대로 396",
                null,
                null,
                null,
                "11번 출구 앞",
                Instant.parse("2026-05-20T05:30:00Z")
        )).isInstanceOf(NullPointerException.class);
    }

    @Test
    void rejectsUpdateWithoutCoordinates() {
        Meeting meeting = Meeting.create(
                Groups.builder().build(),
                MatchedMember.builder().build(),
                ExchangeRound.FIRST_EXCHANGE,
                "강남역",
                "서울특별시 강남구 강남대로 396",
                null,
                new BigDecimal("127.027621"),
                new BigDecimal("37.497942"),
                "11번 출구 앞",
                Instant.parse("2026-05-20T05:30:00Z")
        );

        assertThatThrownBy(() -> meeting.update(
                "강남역",
                "서울특별시 강남구 강남대로 396",
                null,
                null,
                null,
                "11번 출구 앞",
                Instant.parse("2026-05-21T05:30:00Z")
        )).isInstanceOf(NullPointerException.class);
    }
}
