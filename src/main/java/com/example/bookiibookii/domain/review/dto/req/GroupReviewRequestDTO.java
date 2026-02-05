package com.example.bookiibookii.domain.review.dto.req;

import com.example.bookiibookii.domain.user.enums.Badge;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class GroupReviewRequestDTO {

    public record CreateGroupReviewDTO(
            @NotNull Double rating,

            @Size(max = 200, message = "리뷰는 200자 이내로 입력해주세요.")
            String comment,
            List<Badge> badgeCodes
    ) {
    }
}
