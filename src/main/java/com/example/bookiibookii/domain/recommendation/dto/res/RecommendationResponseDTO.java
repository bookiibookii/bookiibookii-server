package com.example.bookiibookii.domain.recommendation.dto.res;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

public class RecommendationResponseDTO {
    @Builder
    @Jacksonized
    public record RecommendedGroupDto  (
            Long groupId,
            String bookImageUrl,
            String bookTitle
    ){}
}
