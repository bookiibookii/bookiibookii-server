package com.example.bookiibookii.domain.tracker.dto.req;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryAddressUpdateRequestDTO(
        @NotBlank @Size(max = 50)
        String receiverName,

        @NotBlank @Size(max = 20)
        String phoneNumber,

        @NotBlank @Size(max = 200)
        String address,

        @Size(max = 200)
        String addressDetail,

        @NotBlank @Size(max = 10)
        String zipCode
) {
}
