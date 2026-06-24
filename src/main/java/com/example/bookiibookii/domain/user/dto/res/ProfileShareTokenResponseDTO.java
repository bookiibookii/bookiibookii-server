package com.example.bookiibookii.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "프로필 공유 토큰 발급 응답")
public record ProfileShareTokenResponseDTO(
        @Schema(description = "공유 토큰(UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String shareToken,
        @Schema(description = "웹 공유 페이지 고유 링크", example = "https://bookiibookii.com/share/profile/a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String shareUrl
) {
}
