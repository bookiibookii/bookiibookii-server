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
}
