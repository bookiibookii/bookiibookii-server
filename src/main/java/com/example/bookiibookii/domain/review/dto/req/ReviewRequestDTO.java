package com.example.bookiibookii.domain.review.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewRequestDTO {

    public record BookReviewUpsertDTO(
            @NotNull(message = "별점은 필수입니다.")
            Double star,

            @Size(max = 500, message = "책 리뷰는 500자 이내로 입력해주세요.")
            String comment
    ) {}
}
