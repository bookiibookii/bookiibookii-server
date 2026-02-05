package com.example.bookiibookii.domain.review.dto.req;

import jakarta.validation.constraints.NotNull;

public record BookReviewRequestDTO(
        @NotNull Double rating,
        String comment
) {
}
