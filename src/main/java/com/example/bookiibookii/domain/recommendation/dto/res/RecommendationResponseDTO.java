package com.example.bookiibookii.domain.recommendation.dto.res;

import lombok.Builder;

import java.util.List;

public class RecommendationResponseDTO {
    @Builder
    public record BookmateDto  (
            // TODO : 프로필 이미지 추가
            Long userId,
            String nickname,
            List<String> matchedTags,
            String recentBookTitle
    ){}

    @Builder
    public record RecommendedGroupDto  (
            // TODO : 도서표지 이미지 추가
            Long groupId,
            String bookTitle
    ){}
}
