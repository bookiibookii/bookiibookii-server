package com.example.bookiibookii.domain.tracker.dto.req;

import jakarta.validation.constraints.NotBlank;

public record TrackerShippingRequest(
        @NotBlank(message = "택배사는 필수 입력입니다.")
        String deliveryCompany,

        @NotBlank(message = "송장 번호는 필수 입력입니다.")
        String trackingNumber,

        @NotBlank(message = "인증샷 URL은 필수 입력입니다.")
        String authenticationImageUrl
) {}