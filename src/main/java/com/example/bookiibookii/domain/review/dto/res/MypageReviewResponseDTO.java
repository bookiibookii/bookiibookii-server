package com.example.bookiibookii.domain.review.dto.res;

import com.example.bookiibookii.domain.notification.enums.ExchangeType;
import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public class MypageReviewResponseDTO {

    public record WrittenReviews(
            long totalCount,
            List<WrittenReviewItem> content,
            PageInfo pageInfo
    ) {
    }

    public record WrittenReviewItem(
            Long reviewId,
            Long bookId,
            String bookTitle,
            String author,
            Double rating,
            String content,
            ExchangeType exchangeType,
            String exchangeTypeLabel,
            String reviewedAt
    ) {
    }

    public record ReceivedReviews(
            long positiveCount,
            List<ReceivedReviewItem> content,
            PageInfo pageInfo
    ) {
    }

    public record ReceivedReviewItem(
            Long reviewId,
            Long reviewerId,
            String reviewerNickname,
            @Schema(nullable = true)
            String reviewerProfileImageUrl,
            @Schema(nullable = true)
            MemberReviewReaction partnerReviewType,
            @Schema(nullable = true)
            String partnerReviewLabel,
            @Schema(nullable = true)
            String comment,
            String reviewedAt
    ) {
    }

    public record PageInfo(
            int page,
            int size,
            int totalPages,
            boolean hasNext
    ) {
    }
}
