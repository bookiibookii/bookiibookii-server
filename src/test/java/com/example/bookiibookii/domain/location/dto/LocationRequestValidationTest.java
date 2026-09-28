package com.example.bookiibookii.domain.location.dto;

import com.example.bookiibookii.domain.location.dto.req.UserDeliveryReqDTO;
import com.example.bookiibookii.domain.location.dto.req.UserExchangeReqDTO;
import com.example.bookiibookii.domain.tracker.dto.req.MeetingRequestDTO;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LocationRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsDeliveryWithoutCoordinates() {
        UserDeliveryReqDTO.AddReqDTO request = new UserDeliveryReqDTO.AddReqDTO(
                "우리집",
                "서울특별시 강남구 강남대로 396",
                "06232",
                "101동 1001호",
                "홍길동",
                "010-1234-5678"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsDeliveryWithoutZipCode() {
        UserDeliveryReqDTO.AddReqDTO request = new UserDeliveryReqDTO.AddReqDTO(
                "우리집",
                "서울특별시 강남구 강남대로 396",
                null,
                "101동 1001호",
                "홍길동",
                "010-1234-5678"
        );

        assertThat(validator.validate(request))
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("zipCode"));
    }

    @Test
    void acceptsExchangeWithoutZipCodeWhenCoordinatesExist() {
        UserExchangeReqDTO.AddReqDTO request = new UserExchangeReqDTO.AddReqDTO(
                "강남역",
                "서울특별시 강남구 강남대로 396",
                null,
                new BigDecimal("127.027621"),
                new BigDecimal("37.497942"),
                "11번 출구 앞"
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsExchangeWithoutCoordinates() {
        UserExchangeReqDTO.AddReqDTO request = new UserExchangeReqDTO.AddReqDTO(
                "강남역",
                "서울특별시 강남구 강남대로 396",
                null,
                null,
                null,
                "11번 출구 앞"
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("x", "y");
    }

    @Test
    void rejectsExchangeWhenCoordinatesAreOutOfRange() {
        UserExchangeReqDTO.AddReqDTO request = new UserExchangeReqDTO.AddReqDTO(
                "강남역",
                "서울특별시 강남구 강남대로 396",
                null,
                new BigDecimal("999"),
                new BigDecimal("-999"),
                "11번 출구 앞"
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("x", "y");
    }

    @Test
    void acceptsMeetingSnapshotWithoutZipCode() {
        MeetingRequestDTO request = new MeetingRequestDTO(
                "스타벅스 강남점",
                "서울특별시 강남구 강남대로 100",
                null,
                new BigDecimal("127.027621"),
                new BigDecimal("37.497942"),
                "2층",
                OffsetDateTime.parse("2026-05-20T14:30:00+09:00")
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMeetingSnapshotWithoutRequiredPlaceFields() {
        MeetingRequestDTO request = new MeetingRequestDTO(
                null,
                null,
                null,
                null,
                null,
                "2층",
                null
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("placeName", "address", "x", "y", "meetingAt");
    }

    @Test
    void rejectsMeetingSnapshotWhenCoordinatesAreOutOfRange() {
        MeetingRequestDTO request = new MeetingRequestDTO(
                "스타벅스 강남점",
                "서울특별시 강남구 강남대로 100",
                null,
                new BigDecimal("999"),
                new BigDecimal("-999"),
                "2층",
                OffsetDateTime.parse("2026-05-20T14:30:00+09:00")
        );

        assertThat(validator.validate(request))
                .extracting(violation -> violation.getPropertyPath().toString())
                .contains("x", "y");
    }
}
