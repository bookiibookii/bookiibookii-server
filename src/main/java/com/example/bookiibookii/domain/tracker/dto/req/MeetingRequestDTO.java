package com.example.bookiibookii.domain.tracker.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(
        description = "직접 교환 약속 등록/수정 요청",
        example = """
        {
          "locationId": 1,
          "addressDetail": "2층 창가 자리",
          "scheduledAt": "2026-05-20T14:30:00"
        }
        """
)
public record MeetingRequestDTO(
        @Schema(description = "Location ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "장소 ID는 필수입니다.")
        Long locationId,

        @Schema(description = "Location에는 없는 상세 주소", example = "2층 창가 자리")
        @Size(max = 200, message = "상세 주소는 200자 이내로 입력해주세요.")
        String addressDetail,

        @Schema(description = "약속 일시", example = "2026-05-20T14:30:00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "약속 일시는 필수입니다.")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime scheduledAt
) {
}
