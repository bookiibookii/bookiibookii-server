package com.example.bookiibookii.domain.group.dto.res;

import lombok.Builder;
import lombok.Getter;
import java.time.Instant;

@Getter
@Builder
public class MatchedMemberResponseDTO {

    @Builder
    @Getter
    public static class CompleteReadingResultDTO {
        private Long matchedMemberId;     // 수정된 멤버 ID
        private Integer currentReadingRate; // 최종 독서율 (100)
        private Instant completedAt;  // 완독 시각
    }
}
