package com.example.bookiibookii.domain.tracker.dto.req;

import com.example.bookiibookii.domain.tracker.enums.DeliveryCompany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DeliveryRegisterRequestDTO(
        @NotNull
        DeliveryCompany deliveryCompany,

        @NotBlank
        String trackingNumber
) {
}
