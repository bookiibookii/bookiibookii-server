package com.example.bookiibookii.domain.location.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UserExchangeReqDTO {

    @Schema(name = "UserExchangeAddReqDTO")
    public record AddReqDTO(
            @Schema(description = "장소 이름", example = "강남역", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "장소 이름은 필수입니다.")
            String placeName,

            @Schema(description = "주소", example = "서울특별시 강남구 강남대로 396", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "주소는 필수입니다.")
            String address,

            @Schema(description = "우편번호(선택)", example = "06232", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String zipCode,

            @Schema(description = "X 좌표(경도)", example = "127.027621", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "X 좌표는 필수입니다.")
            @DecimalMin(value = "-180.0", message = "X 좌표는 -180 이상이어야 합니다.")
            @DecimalMax(value = "180.0", message = "X 좌표는 180 이하여야 합니다.")
            BigDecimal x,

            @Schema(description = "Y 좌표(위도)", example = "37.497942", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotNull(message = "Y 좌표는 필수입니다.")
            @DecimalMin(value = "-90.0", message = "Y 좌표는 -90 이상이어야 합니다.")
            @DecimalMax(value = "90.0", message = "Y 좌표는 90 이하여야 합니다.")
            BigDecimal y,

            @Schema(description = "상세 주소/설명", example = "11번 출구 앞")
            String addressDetail
    ) {}
}
