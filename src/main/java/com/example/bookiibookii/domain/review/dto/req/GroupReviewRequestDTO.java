package com.example.bookiibookii.domain.review.dto.req;

import jakarta.validation.constraints.NotNull;

public class GroupReviewRequestDTO {

    public record CreateGroupReviewDTO(
            @NotNull Double rating,
            String comment,
            java.util.List<com.example.bookiibookii.domain.user.enums.Badge> badgeCodes
    ) {
    }
}
