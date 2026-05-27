package com.example.bookiibookii.domain.tracker.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "트래커 목록 응답")
public record TrackerListResDTO(
        @Schema(description = "상단 상태별 카운트")
        Summary summary,

        @Schema(description = "트래커 카드 목록")
        List<TrackerListItemResDTO> items
) {

    @Builder
    @Schema(description = "트래커 상태별 카운트 요약")
    public record Summary(
            @Schema(description = "전체 트래커 수", example = "8")
            int totalCount,

            @Schema(description = "읽는 중 트래커 수", example = "3")
            int readingCount,

            @Schema(description = "교환 중 트래커 수", example = "2")
            int exchangingCount,

            @Schema(description = "후기 작성 단계 트래커 수", example = "3")
            int reviewCount
    ) {
    }
}
