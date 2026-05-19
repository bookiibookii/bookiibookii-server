package com.example.bookiibookii.domain.tracker.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record TrackerShippingRequestDTO(
        @Schema(description = "택배사 명", example = "CJ대한통운")
        @NotBlank(message = "택배사는 필수 입력입니다.")
        String deliveryCompany,

        @Schema(description = "운송장 번호", example = "123456789012")
        @NotBlank(message = "송장 번호는 필수 입력입니다.")
        String trackingNumber
) {}