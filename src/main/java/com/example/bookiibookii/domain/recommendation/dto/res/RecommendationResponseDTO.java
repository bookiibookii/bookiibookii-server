package com.example.bookiibookii.domain.recommendation.dto.res;

import com.example.bookiibookii.domain.user.entity.UserImage;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

public class RecommendationResponseDTO {
    @Builder
    public record BookmateDto  (
            UserImage userImage,
            Long userId,
            String nickname,
            List<String> matchedTags,
            String recentBookTitle
    ){}

    @Builder
    @Jacksonized
    public record RecommendedGroupDto  (
            // TODO : 도서표지 이미지 추가
            Long groupId,
            String bookTitle
    ){}
}
