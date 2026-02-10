package com.example.bookiibookii.domain.review.converter;

import com.example.bookiibookii.domain.group.entity.Groups;
import com.example.bookiibookii.domain.group.entity.MatchedMember;
import com.example.bookiibookii.domain.review.dto.res.GroupReviewResponseDTO;
import com.example.bookiibookii.domain.review.entity.GroupReview;
import com.example.bookiibookii.domain.tracker.entity.Tracker;
import com.example.bookiibookii.domain.userbook.entity.UserBook;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReviewConverter {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy. MM. dd.");

    public GroupReviewResponseDTO.GroupReviewDetailDTO toGroupReviewDetailDTO(
            List<GroupReviewResponseDTO.GroupReviewDetailDTO.MyReviewItemDTO> reviewItems) {
        return GroupReviewResponseDTO.GroupReviewDetailDTO.builder()
                .reviews(reviewItems)
                .build();
    }

    public GroupReviewResponseDTO.GroupReviewDetailDTO.MyReviewItemDTO toMyReviewItemDTO(
            Groups group, Tracker tracker, MatchedMember partnerMM, GroupReview gr, UserBook pub) {

        // 배지 정보 변환
        List<GroupReviewResponseDTO.BadgeInfo> badges = (gr != null) ? gr.getBadges().stream()
                .map(b -> GroupReviewResponseDTO.BadgeInfo.builder()
                        .code(b.getBadge().name())
                        .description(b.getBadge().getDescription())
                        .build())
                .toList() : List.of();

        return GroupReviewResponseDTO.GroupReviewDetailDTO.MyReviewItemDTO.builder()
                .groupId(group.getGroupId())
                .bookTitle(group.getBook().getTitle())
                .bookImage(group.getBook().getImage())
                .startDate(group.getStartDate().format(DATE_FMT))
                // 종료일 처리 (Tracker 기준)
                .finishedDate(tracker != null && tracker.getUpdatedAt() != null ?
                        tracker.getUpdatedAt().format(DATE_FMT) : "진행중")
                .partnerNickname(partnerMM != null ? partnerMM.getUser().getNickName() : "알 수 없음")
                // 상대방이 나에게 남긴 리뷰 정보
                .partnerToMeRating(gr != null ? gr.getRating() : 0.0)
                .partnerToMeComment(gr != null ? gr.getComment() : "평가가 없습니다.")
                .partnerBadges(badges)
                // 상대방이 쓴 책 리뷰 정보
                .partnerBookRating(pub != null ? pub.getRating() : 0.0)
                .partnerBookComment(pub != null ? pub.getComment() : "리뷰가 없습니다.")
                .partnerBookReviewDate(pub != null && pub.getUpdatedAt() != null ?
                        pub.getUpdatedAt().format(DATE_FMT) : null)
                .build();
    }
}
