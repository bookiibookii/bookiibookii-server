package com.example.bookiibookii.domain.memberbook.dto.res;

import com.example.bookiibookii.domain.memberbook.enums.CardType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class MemberCardCreateResponseDTO {
    private Long cardId;
    private CardType cardType;
    private Integer page;
    private String memo;
    private String quotation;
    private MemberCardImageResponseDTO cardImage;
    private Instant createdAt;
    private String creatorName;
    private String creatorProfileImageUrl;
}
