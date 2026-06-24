package com.example.bookiibookii.domain.tracker.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(name = "DeliveryAddressSavedUpdateRequestDTO")
public record DeliveryAddressSavedUpdateRequestDTO(
        @Schema(description = "마이페이지에 등록된 배송지 식별자(user_deliveryID)", example = "1")
        @NotNull @Positive
        Long userDeliveryId
) {
}
