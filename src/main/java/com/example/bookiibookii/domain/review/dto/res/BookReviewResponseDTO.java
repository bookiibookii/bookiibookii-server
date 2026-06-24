package com.example.bookiibookii.domain.review.dto.res;

import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "책 리뷰 등록/수정 응답")
public record BookReviewResponseDTO(
        @Schema(description = "책 리뷰 식별자(ID)", example = "10")
        Long reviewId,
        @Schema(description = "그룹 식별자(ID)", example = "1")
        Long groupId,
        @Schema(description = "리뷰 대상 memberBook 식별자(ID)", example = "100")
        Long memberBookId,
        @Schema(description = "별점", example = "4.5")
        Double star,
        @Schema(description = "책 리뷰 코멘트", example = "문장이 좋아서 오래 기억에 남았어요")
        String comment,
        @Schema(description = "리뷰 등록/수정 후 독서 상태", example = "EXCHANGING")
        ReadingStatus readingStatus,
        @Schema(description = "리뷰 등록/수정 후 교환 상태", example = "TRACKING_REGISTER_WAITING")
        ExchangeStatus exchangeStatus
) {

    public static BookReviewResponseDTO from(BookReview review) {
        return BookReviewResponseDTO.builder()
                .reviewId(review.getId())
                .groupId(review.getMatchedMember().getGroup().getId())
                .memberBookId(review.getMemberBook().getId())
                .star(review.getStar())
                .comment(review.getComment())
                .readingStatus(review.getMatchedMember().getReadingStatus())
                .exchangeStatus(review.getMatchedMember().getExchangeStatus())
                .build();
    }
}
