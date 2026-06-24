package com.example.bookiibookii.domain.review.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "내 그룹 리뷰 수정 응답")
public record MyGroupReviewsResponseDTO(
        @Schema(description = "수정 후 내 책 리뷰 목록")
        List<GroupReviewsResponseDTO.BookReviewItem> bookReviews,
        @Schema(description = "수정 후 내 파트너 리뷰")
        GroupReviewsResponseDTO.MemberReviewItem memberReview
) {
}
