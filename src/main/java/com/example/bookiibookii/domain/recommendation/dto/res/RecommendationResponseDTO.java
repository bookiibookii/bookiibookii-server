package com.example.bookiibookii.domain.recommendation.dto.res;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class RecommendationResponseDTO {
    @Builder
    public record BookmateDto(
            Long userId,
            String nickname,
            /** 프로필 이미지 Presigned GET URL. 미등록 시 null */
            String profileImageUrl,
            List<String> matchedTags,
            String recentBookTitle
    ) {}

    @Builder
    @Jacksonized
    public record RecommendedGroupDto  (
            // TODO : 도서표지 이미지 추가
            Long groupId,
            String bookTitle
    ){}
}
