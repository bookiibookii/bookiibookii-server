package com.example.bookiibookii.domain.tracker.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "직접 교환 약속 상세 응답")
public record TrackerMeetingResponseDTO(
        @Schema(description = "교환 일시", example = "2026-02-05T14:30:00")
        LocalDateTime meetingTime,
        @Schema(description = "교환 장소", example = "강남역 2번 출구 앞")
        String meetingPlace


) {}