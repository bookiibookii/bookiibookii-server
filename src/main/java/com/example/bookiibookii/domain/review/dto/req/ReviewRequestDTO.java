package com.example.bookiibookii.domain.review.dto.req;

import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ReviewRequestDTO {

    public record BookReviewUpsertDTO(
            @Schema(description = "별점. 0.0~5.0 범위에서 0.5 단위만 허용됩니다.", example = "4.5", minimum = "0.0", maximum = "5.0")
            @NotNull(message = "별점은 필수입니다.")
            Double star,

            @Schema(description = "책 리뷰 코멘트. 선택값이며 최대 500자입니다.", example = "문장이 좋아서 오래 기억에 남았어요", maxLength = 500)
            @Size(max = 500, message = "책 리뷰는 500자 이내로 입력해주세요.")
            String comment
    ) {}

    public record MemberReviewCreateDTO(
            @Schema(
                    description = "파트너 후기 리액션. 선택값이며 null 또는 생략 가능합니다.",
                    example = "BOOM_UP",
                    allowableValues = {"BOOM_UP", "BOOM_DOWN"},
                    nullable = true
            )
            MemberReviewReaction reaction,

            @Schema(description = "파트너 후기 코멘트. 필수값이며 최대 20자입니다.", example = "좋았어요", maxLength = 20, requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "코멘트는 필수입니다.")
            @Size(max = 20, message = "파트너 후기는 20자 이내로 입력해주세요.")
            String comment
    ) {}
}
