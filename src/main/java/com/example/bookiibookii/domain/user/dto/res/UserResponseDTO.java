package com.example.bookiibookii.domain.user.dto.res;

import com.example.bookiibookii.domain.group.enums.TradeType;
import com.example.bookiibookii.domain.review.enums.MemberReviewReaction;
import com.example.bookiibookii.domain.user.enums.NicknameStatus;
import lombok.Builder;

import java.util.List;

public class UserResponseDTO {

    @Builder
    public record UserProfileResDTO(
            Long userId,
            String profileImageUrl,
            String nickname,
            String introduction,
            List<UserBookDto> userBooks,
            Integer bookReviewCount,
            List<BookReviewSummaryDto> recentBookReviews,
            Integer boomUpCount,
            List<ReceivedMemberReviewDto> recentReceivedReviews
    ) {}

    public record UserBookDto(
            String title,
            String auth,
            String image
    ) {}

    @Builder
    public record BookReviewSummaryDto(
            String bookTitle,
            String bookAuthor,
            TradeType tradeType,
            Double rating,
            String comment,
            String reviewDate
    ) {}

    @Builder
    public record ReceivedMemberReviewDto(
            String reviewerNickname,
            String reviewerProfileUrl,
            MemberReviewReaction reaction,
            String comment,
            String createdAt
    ) {}

    @Builder
    public record NicknameValidationDTO(
            boolean isAvailable,
            String code,
            String message
    ) {
        public static NicknameValidationDTO from(NicknameStatus status) {
            return switch (status) {
                case AVAILABLE -> NicknameValidationDTO.builder()
                        .isAvailable(true)
                        .code("SUCCESS")
                        .message("사용 가능한 닉네임입니다.")
                        .build();
                case DUPLICATE -> NicknameValidationDTO.builder()
                        .isAvailable(false)
                        .code("DUPLICATE")
                        .message("이미 사용 중인 닉네임입니다.")
                        .build();
                case BAD_WORD -> NicknameValidationDTO.builder()
                        .isAvailable(false)
                        .code("BAD_WORD")
                        .message("금칙어가 포함되어 있습니다.")
                        .build();
            };
        }
    }
}
