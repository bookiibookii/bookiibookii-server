package com.example.bookiibookii.domain.userbook.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CardCreateResponseDTO {
    private Long cardId;
    private Integer page;
    private String memo;
    private CardImageResponseDTO cardImage;
    private LocalDateTime createdAt;
    /** 책 제목 (카드 상세 조회 시에만 설정, 그 외에는 null) */
    private String bookTitle;
}
