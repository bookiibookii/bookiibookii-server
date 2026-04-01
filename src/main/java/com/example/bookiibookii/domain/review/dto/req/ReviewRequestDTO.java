package com.example.bookiibookii.domain.review.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ReviewRequestDTO {

    /**
     * 1. 함께 읽기(Together) 전용 리뷰 DTO
     * - 책에 대한 별점과 코멘트만 포함합니다.
     */
    public record TogetherReviewDTO(
            @NotNull(message = "평점은 필수입니다.")
            Double rating,

            @Size(max = 500, message = "리뷰는 500자 이내로 입력해주세요.")
            String comment
    ) {}

    /**
     * 2. 릴레이(Relay) 전용 통합 리뷰 DTO
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