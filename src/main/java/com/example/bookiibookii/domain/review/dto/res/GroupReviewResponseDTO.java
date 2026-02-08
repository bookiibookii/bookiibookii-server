package com.example.bookiibookii.domain.review.dto.res;

import com.example.bookiibookii.domain.user.enums.Badge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class GroupReviewResponseDTO {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GroupReviewDetailDTO {
        private List<MyReviewItemDTO> reviews;

        @Builder
        @Getter
        @NoArgsConstructor
        @AllArgsConstructor
        public static class MyReviewItemDTO {
            private Long groupId;
            private String bookTitle;
            private String bookImage;
            private String startDate;
            private String finishedDate; // 그룹 종료일

            // 상대방 정보
            private String partnerNickname;

            // 상대방이 '나'에게 남긴 매너 평가 (GroupReview)
            private Double partnerToMeRating;    // 나에게 준 매너 점수
            private String partnerToMeComment;   // 나에게 남긴 말
            private List<BadgeInfo> partnerBadges;// 나에게 준 뱃지 리스트

            // 상대방이 '책'에 대해 남긴 리뷰 (Partner's UserBook)
            private Double partnerBookRating;    // 상대가 책에 준 별점
            private String partnerBookComment;   // 상대가 책에 대해 쓴 감상문
            private String partnerBookReviewDate; // 상대가 책 리뷰를 쓴 날짜

        }
    }

    @Builder
    @Getter
    public static class BadgeInfo {
        private String code;
        private String description;
    }
}
