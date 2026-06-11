package com.example.bookiibookii.domain.review.dto.req;

import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

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

            @Schema(description = "파트너 후기 코멘트. 선택값이며 최대 20자입니다.", example = "좋았어요", maxLength = 20, nullable = true)
            @Size(max = 20, message = "파트너 후기는 20자 이내로 입력해주세요.")
            String comment
    ) {}

    public record MyGroupReviewsUpdateDTO(
            @Schema(description = "수정할 책 리뷰 목록. memberBookId 기준으로 별점/코멘트를 부분 수정할 수 있습니다.")
            List<BookReviewUpdateItem> bookReviews,

            @Schema(description = "수정할 파트너 리뷰. reaction/comment를 부분 수정할 수 있습니다.")
            MemberReviewUpdateItem memberReview
    ) {

        public record BookReviewUpdateItem(
                @Schema(description = "수정 대상 memberBook 식별자(ID)", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
                @NotNull(message = "memberBookId는 필수입니다.")
                Long memberBookId,

                @Schema(description = "변경할 별점. 생략 시 기존 값을 유지합니다.", example = "4.5", minimum = "0.0", maximum = "5.0")
                Double star,

                @Schema(description = "변경할 코멘트. 생략 시 기존 값을 유지합니다.", example = "다시 읽어도 좋았어요", maxLength = 500)
                @Size(max = 500, message = "책 리뷰는 500자 이내로 입력해주세요.")
                String comment
        ) {}

        public record MemberReviewUpdateItem(
                @Schema(
                        description = "변경할 파트너 후기 리액션. 생략 시 기존 값을 유지합니다.",
                        example = "BOOM_UP",
                        allowableValues = {"BOOM_UP", "BOOM_DOWN"},
                        nullable = true
                )
                MemberReviewReaction reaction,

                @Schema(description = "변경할 파트너 후기 코멘트. 생략 시 기존 값을 유지합니다.", example = "좋았어요", maxLength = 20)
                @Size(max = 20, message = "파트너 후기는 20자 이내로 입력해주세요.")
                String comment
        ) {}
    }
}
