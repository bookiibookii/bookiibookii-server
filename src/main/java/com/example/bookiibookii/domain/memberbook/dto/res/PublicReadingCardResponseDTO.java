package com.example.bookiibookii.domain.memberbook.dto.res;

import com.example.bookiibookii.domain.memberbook.enums.CardType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "공유 토큰 기반 독서카드 공개 조회 응답")
public record PublicReadingCardResponseDTO(
        @Schema(description = "카드 타입", example = "TEXT")
        CardType cardType,
        @Schema(description = "책 제목", example = "어린 왕자")
        String bookTitle,
        @Schema(description = "책 저자", example = "앙투안 드 생텍쥐페리")
        String bookAuthor,
        @Schema(description = "책 표지 이미지 URL", example = "https://example.com/book.jpg")
        String bookImage,
        @Schema(description = "작성자 닉네임", example = "booklover")
        String creatorNickname,
        @Schema(description = "페이지", example = "42")
        Integer page,
        @Schema(description = "메모", example = "이 구절이 인상 깊었어요")
        String memo,
        @Schema(description = "인용문", example = "중요한 것은 눈에 보이지 않아")
        String quotation,
        @Schema(description = "IMAGE 타입 카드 이미지 Presigned URL")
        String imageUrl
) {
}
