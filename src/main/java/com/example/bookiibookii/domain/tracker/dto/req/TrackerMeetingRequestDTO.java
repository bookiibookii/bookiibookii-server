package com.example.bookiibookii.domain.tracker.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Schema(description = "직접 교환 약속 등록 및 수정 요청")
public record TrackerMeetingRequestDTO(

        @Schema(description = "교환 예정 일시", example = "2026-02-05T14:30:00")
        @NotNull(message = "교환 일시는 필수 입력 사항입니다.")
        @Future(message = "약속 시간은 현재 시간보다 이후여야 합니다.") // 과거 시간 등록 방지
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm"
        )
        LocalDateTime meetingTime,

        @Schema(description = "교환 희망 장소", example = "강남역 2번 출구 앞")
        @NotBlank(message = "교환 장소는 필수 입력 사항입니다.")
        String meetingPlace

) {}