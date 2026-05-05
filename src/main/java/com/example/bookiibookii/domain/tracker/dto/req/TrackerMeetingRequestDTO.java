package com.example.bookiibookii.domain.tracker.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "직접 교환 약속 등록 및 수정 요청")
public record TrackerMeetingRequestDTO(

        @Schema(description = "교환 예정 일시 (분 단위까지)", example = "2026-02-28T14:30:00")
        @NotNull(message = "교환 일시는 필수 입력 사항입니다.")
        @Future(message = "약속 시간은 현재 시간보다 이후여야 합니다.")
        @JsonFormat(
                shape = JsonFormat.Shape.STRING,
                pattern = "yyyy-MM-dd'T'HH:mm:ss[X]"
        )
        LocalDateTime meetingTime,

        @Schema(description = "장소명", example = "강남역 2번 출구 앞")
        @NotBlank(message = "장소명은 필수 입력 사항입니다.")
        String placeName,

        @Schema(description = "주소", example = "서울특별시 강남구 강남대로 396")
        String address,

        @Schema(description = "상세 주소", example = "2번 출구")
        String addressDetail,

        @Schema(description = "우편번호", example = "06232")
        @Size(max = 5, message = "우편번호는 5자리입니다.")
        String zipCode

) {}
