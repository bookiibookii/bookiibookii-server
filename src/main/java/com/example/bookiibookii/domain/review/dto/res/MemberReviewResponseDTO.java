package com.example.bookiibookii.domain.review.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "파트너 후기 등록 응답")
public record MemberReviewResponseDTO(
        @Schema(description = "파트너 후기 식별자(ID)", example = "10")
        Long reviewId,
        @Schema(description = "이번 후기 등록으로 그룹이 최종 종료되었는지 여부", example = "false")
        boolean groupCompleted
) {
}
