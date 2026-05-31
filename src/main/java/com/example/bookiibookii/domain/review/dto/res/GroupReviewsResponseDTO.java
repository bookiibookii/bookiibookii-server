package com.example.bookiibookii.domain.review.dto.res;

import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "그룹 리뷰 조회 응답")
public record GroupReviewsResponseDTO(
        @Schema(description = "그룹 내 책 리뷰 목록")
        List<BookReviewItem> bookReviews,
        @Schema(description = "그룹 내 파트너 리뷰 목록")
        List<MemberReviewItem> memberReviews
) {

    @Builder
    @Schema(description = "책 리뷰 항목")
    public record BookReviewItem(
            @Schema(description = "책 식별자(ID)", example = "1")
            Long bookId,
            @Schema(description = "책 제목", example = "어린 왕자")
            String bookTitle,
            @Schema(description = "책 저자", example = "앙투안 드 생텍쥐페리")
            String bookAuthor,
            @Schema(description = "책 표지 이미지 URL", example = "https://example.com/book.jpg")
            String bookImage,
            @Schema(description = "작성자 사용자 식별자(ID)", example = "10")
            Long writerId,
            @Schema(description = "작성자 닉네임", example = "booklover")
            String writerNickname,
            @Schema(description = "작성자 프로필 이미지 Presigned URL")
            String writerProfileImageUrl,
            @Schema(description = "별점", example = "4.5")
            Double star,
            @Schema(description = "리뷰 내용", example = "문장이 좋아서 오래 기억에 남았어요")
            String comment,
            @Schema(description = "작성 일자", example = "2026. 05. 31.")
            String createdAt
    ) {}

    @Builder
    @Schema(description = "파트너 리뷰 항목")
    public record MemberReviewItem(
            @Schema(description = "그룹명", example = "5월 교환독서")
            String groupName,
            @Schema(description = "그룹 독서 기간(일)", example = "14")
            Integer readingPeriod,
            @Schema(description = "작성자 사용자 식별자(ID)", example = "10")
            Long writerId,
            @Schema(description = "작성자 닉네임", example = "booklover")
            String writerNickname,
            @Schema(description = "작성자 프로필 이미지 Presigned URL")
            String writerProfileImageUrl,
            @Schema(description = "파트너 후기 리액션", example = "BOOM_UP", allowableValues = {"BOOM_UP", "BOOM_DOWN"}, nullable = true)
            MemberReviewReaction reaction,
            @Schema(description = "파트너에게 남긴 코멘트", example = "좋았어요")
            String comment
    ) {}
}
