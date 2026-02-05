package com.example.bookiibookii.domain.review.dto.req;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
public record BookReviewRequestDTO(
        @NotNull Double rating,

        @Size(max = 500, message = "리뷰는 500자 이내로 입력해주세요.")
        String comment
) {
}
