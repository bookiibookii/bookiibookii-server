package com.example.bookiibookii.global.config;

import com.example.bookiibookii.domain.tracker.dto.res.MeetingResponseDTO;
import com.example.bookiibookii.domain.tracker.enums.ExchangeRound;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonConfigTest {

    private final JacksonConfig jacksonConfig = new JacksonConfig();

    @Test
    void serializesInstantAsIso8601String() throws Exception {
        MeetingResponseDTO response = MeetingResponseDTO.builder()
                .meetingId(10L)
                .exchangeRound(ExchangeRound.FIRST_EXCHANGE)
                .meetingAt(Instant.parse("2026-06-24T09:00:00Z"))
                .build();

        String json = jacksonConfig.objectMapper().writeValueAsString(response);

        assertThat(json).contains("\"meetingAt\":\"2026-06-24T09:00:00Z\"");
    }
}
