package com.example.bookiibookii.domain.userbook.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardCreateResponseDTO {
    private Long cardId;
    private Integer page;
    private String memo;
    private CardImageResponseDTO cardImage;
}
