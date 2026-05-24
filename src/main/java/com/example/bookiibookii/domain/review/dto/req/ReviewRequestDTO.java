package com.example.bookiibookii.domain.review.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewRequestDTO {

    /**
     * 릴레이 중간 책 리뷰 DTO (1차/2차 독서 완료 후 교환·반납 전)
     */
    public record BookReviewDTO(
            @NotNull(message = "책 평점은 필수입니다.")
            Double bookRating,

            @Size(max = 500, message = "책 리뷰는 500자 이내로 입력해주세요.")
            String bookComment
    ) {}

    public record BookReviewUpsertDTO(
            @NotNull(message = "별점은 필수입니다.")
            Double star,

            @Size(max = 500, message = "책 리뷰는 500자 이내로 입력해주세요.")
            String comment
    ) {}

    /**
     * 2-B. 릴레이(Relay) 전용 통합 리뷰 DTO (릴레이 종료 후 파트너 리뷰 포함)
     * - 책 리뷰 필드와 파트너(상대방) 리뷰 필드를 모두 포함합니다.
     */
    public record RelayReviewDTO(
            // [책 관련 리뷰]
            @NotNull(message = "책 평점은 필수입니다.")
            Double bookRating,

            @Size(max = 500, message = "책 리뷰는 500자 이내로 입력해주세요.")
            String bookComment,

            // [파트너 관련 리뷰]
            @NotNull(message = "파트너 평점은 필수입니다.")
            Double partnerRating,

            @Size(max = 200, message = "파트너 리뷰는 200자 이내로 입력해주세요.")
            String partnerComment
    ) {}
}
