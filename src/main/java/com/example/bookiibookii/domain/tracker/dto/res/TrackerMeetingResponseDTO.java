package com.example.bookiibookii.domain.tracker.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

@Schema(description = "직접 교환 약속 상세 응답")
public record TrackerMeetingResponseDTO(
        @Schema(description = "교환 일시", example = "2026-02-05T14:30:00")
        Instant meetingTime,
        @Schema(description = "장소명", example = "강남역 2번 출구 앞")
        String placeName,
        @Schema(description = "주소", example = "서울특별시 강남구 강남대로 396")
        String address
) {}
