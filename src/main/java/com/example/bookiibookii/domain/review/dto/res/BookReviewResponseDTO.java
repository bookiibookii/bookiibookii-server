package com.example.bookiibookii.domain.review.dto.res;

import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.tracker.enums.ExchangeStatus;
import com.example.bookiibookii.domain.tracker.enums.ReadingStatus;
import lombok.Builder;

@Builder
public record BookReviewResponseDTO(
        Long reviewId,
        Long groupId,
        Long memberBookId,
        Double star,
        String comment,
        ReadingStatus readingStatus,
        ExchangeStatus exchangeStatus
) {

    public static BookReviewResponseDTO from(BookReview review) {
        return BookReviewResponseDTO.builder()
                .reviewId(review.getId())
                .groupId(review.getMatchedMember().getGroup().getGroupId())
                .memberBookId(review.getMemberBook().getId())
                .star(review.getStar())
                .comment(review.getComment())
                .readingStatus(review.getMatchedMember().getReadingStatus())
                .exchangeStatus(review.getMatchedMember().getExchangeStatus())
                .build();
    }
}
