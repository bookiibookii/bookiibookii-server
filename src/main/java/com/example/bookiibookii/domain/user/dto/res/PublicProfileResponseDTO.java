package com.example.bookiibookii.domain.user.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "공유 토큰 기반 프로필 공개 조회 응답")
public record PublicProfileResponseDTO(
        @Schema(description = "닉네임", example = "booklover")
        String nickname,
        @Schema(description = "프로필 이미지 Presigned URL")
        String profileImageUrl,
        @Schema(description = "한줄 소개", example = "책과 함께하는 일상을 기록합니다.")
        String introduction,
        @Schema(description = "나를 대표하는 책 목록 (displayOrder 순)")
        List<RepresentativeBookDto> representativeBooks
) {
    @Builder
    @Schema(description = "나를 대표하는 책")
    public record RepresentativeBookDto(
            @Schema(description = "책 제목", example = "어린 왕자")
            String title,
            @Schema(description = "책 저자", example = "앙투안 드 생텍쥐페리")
            String author,
            @Schema(description = "책 표지 이미지 URL", example = "https://example.com/book.jpg")
            String image,
            @Schema(description = "대표책 표시 순서", example = "1")
            Integer displayOrder,
            @Schema(description = "책 후기 별점", example = "4.5", nullable = true)
            Double rating
    ) {
    }
}
