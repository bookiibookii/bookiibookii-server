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

    // TODO: 추천그룹DTO(groupID, 도서표지 이미지, BookTitle-해당 그룹의 책 제목)
}
