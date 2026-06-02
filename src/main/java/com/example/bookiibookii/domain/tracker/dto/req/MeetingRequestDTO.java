package com.example.bookiibookii.domain.tracker.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(
        description = "직접 교환 약속 등록/수정 요청",
        example = """
        {
          "placeName": "스타벅스 강남점",
          "address": "서울특별시 강남구 강남대로 100",
          "x": 127.027621,
          "y": 37.497942,
          "addressDetail": "2층",
          "scheduledAt": "2026-05-20T14:30:00"
        }
        """
)
public record MeetingRequestDTO(
        @Schema(description = "장소명", example = "강남역", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "장소 이름은 필수입니다.")
        @Size(max = 100, message = "장소 이름은 100자 이내로 입력해주세요.")
        String placeName,

        @Schema(description = "주소", example = "서울특별시 강남구 강남대로 396", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "주소는 필수입니다.")
        @Size(max = 200, message = "주소는 200자 이내로 입력해주세요.")
        String address,

        @Schema(description = "우편번호(선택)", example = "06232", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Size(max = 10, message = "우편번호는 10자 이내로 입력해주세요.")
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

        @Schema(description = "약속별 상세 주소/설명", example = "2층 창가 자리")
        @Size(max = 200, message = "상세 주소는 200자 이내로 입력해주세요.")
        String addressDetail,

        @Schema(description = "약속 일시", example = "2026-05-20T14:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "약속 일시는 필수입니다.")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime scheduledAt
) {
}
