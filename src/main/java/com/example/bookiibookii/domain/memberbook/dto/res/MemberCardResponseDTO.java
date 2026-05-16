package com.example.bookiibookii.domain.memberbook.dto.res;

import com.example.bookiibookii.domain.memberbook.enums.CardType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberCardResponseDTO {
    private Long cardId;
    private Long memberBookId;
    private CardType cardType;
    private Integer page;
    private String memo;
    private String quotation;
    private MemberCardImageResponseDTO cardImage;
    private LocalDateTime createdAt;
    private String bookTitle;
    private Boolean isMine;
    private Boolean isBookmarked;
    private String creatorName;
}
