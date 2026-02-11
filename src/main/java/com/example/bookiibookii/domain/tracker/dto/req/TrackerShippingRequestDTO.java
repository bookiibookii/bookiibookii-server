package com.example.bookiibookii.domain.tracker.dto.req;

import jakarta.validation.constraints.NotBlank;

public record TrackerShippingRequestDTO(
        @NotBlank(message = "택배사는 필수 입력입니다.")
        String deliveryCompany,

        @NotBlank(message = "송장 번호는 필수 입력입니다.")
        String trackingNumber,

        @NotBlank(message = "배송 인증 이미지 S3 키는 필수 입력입니다. Presigned URL로 업로드 후 발급받은 s3Key를 전달하세요.")
        String s3Key
) {}