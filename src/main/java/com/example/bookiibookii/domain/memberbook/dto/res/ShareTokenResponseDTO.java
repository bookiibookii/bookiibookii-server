package com.example.bookiibookii.domain.memberbook.dto.res;

import com.example.bookiibookii.domain.memberbook.enums.ShareLayout;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "독서카드 공유 토큰 발급 응답")
public record ShareTokenResponseDTO(
        @Schema(description = "공유 토큰(UUID)", example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String shareToken,
        @Schema(description = "웹 공유 페이지 고유 링크", example = "https://bookiibookii.com/share/reading-card/a1b2c3d4-e5f6-7890-abcd-ef1234567890")
        String shareUrl,
        @Schema(description = "저장된 링크 공유 레이아웃", example = "OVERLAY")
        ShareLayout shareLayout
) {
}
