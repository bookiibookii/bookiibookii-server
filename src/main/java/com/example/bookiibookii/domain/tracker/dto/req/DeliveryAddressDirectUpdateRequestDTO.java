package com.example.bookiibookii.domain.tracker.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "DeliveryAddressDirectUpdateRequestDTO")
public record DeliveryAddressDirectUpdateRequestDTO(
        @Schema(description = "우편번호", example = "06234")
        @NotBlank @Size(max = 10)
        @Pattern(regexp = "\\d{5}")
        String zipCode,

        @Schema(description = "주소", example = "서울특별시 강남구 테헤란로 123")
        @NotBlank @Size(max = 200)
        String address,

        @Schema(description = "상세 주소", example = "북키빌딩 456호")
        @Size(max = 200)
        String addressDetail
) {
}
