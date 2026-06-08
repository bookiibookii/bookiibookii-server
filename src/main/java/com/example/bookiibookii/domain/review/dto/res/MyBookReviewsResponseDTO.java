package com.example.bookiibookii.domain.review.dto.res;

import com.example.bookiibookii.domain.review.entity.BookReview;
import com.example.bookiibookii.domain.review.enums.BookReviewType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
@Schema(description = "내 책 리뷰 목록 조회 응답")
public record MyBookReviewsResponseDTO(
        @Schema(description = "내가 해당 그룹에서 작성한 책 리뷰 목록")
        List<BookReviewItem> reviews
) {

    @Builder
    @Schema(description = "내 책 리뷰 항목")
    public record BookReviewItem(
            @Schema(description = "책 리뷰 식별자(ID)", example = "10")
            Long reviewId,
            @Schema(description = "책 리뷰 구분", example = "MY_BOOK")
            BookReviewType reviewType,
            @Schema(description = "그룹 식별자(ID)", example = "1")
            Long groupId,
            @Schema(description = "책 식별자(ID)", example = "20")
            Long bookId,
            @Schema(description = "책 제목", example = "어린 왕자")
            String bookTitle,
            @Schema(description = "책 저자", example = "앙투안 드 생텍쥐페리")
            String bookAuthor,
            @Schema(description = "책 표지 이미지 URL", example = "https://example.com/book.jpg")
            String bookImageUrl,
            @Schema(description = "별점", example = "4.5")
            Double rating,
            @Schema(description = "책 리뷰 내용", example = "문장이 좋아서 오래 기억에 남았어요")
            String content,
            @Schema(description = "수정 가능 여부", example = "true")
            @JsonProperty("isEditable")
            boolean isEditable,
            @Schema(description = "리뷰 작성 시각", example = "2026-06-08T10:30:00")
            LocalDateTime createdAt,
            @Schema(description = "리뷰 수정 시각", example = "2026-06-08T11:00:00")
            LocalDateTime updatedAt
    ) {

        public static BookReviewItem from(BookReview review) {
            var memberBook = review.getMemberBook();
            var book = memberBook.getBook();

            return BookReviewItem.builder()
                    .reviewId(review.getId())
                    .reviewType(memberBook.isMyBook() ? BookReviewType.MY_BOOK : BookReviewType.PARTNER_BOOK)
                    .groupId(review.getMatchedMember().getGroup().getId())
                    .bookId(book.getId())
                    .bookTitle(book.getTitle())
                    .bookAuthor(book.getAuthor())
                    .bookImageUrl(book.getImage())
                    .rating(review.getStar())
                    .content(review.getComment())
                    .isEditable(true)
                    .createdAt(review.getCreatedAt())
                    .updatedAt(review.getUpdatedAt())
                    .build();
        }
    }
}
