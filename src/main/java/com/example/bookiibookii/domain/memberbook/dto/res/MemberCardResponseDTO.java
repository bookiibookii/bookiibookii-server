package com.example.bookiibookii.domain.memberbook.dto.res;

import com.example.bookiibookii.domain.memberbook.enums.CardReactionType;
import com.example.bookiibookii.domain.memberbook.enums.CardType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

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
    private String creatorProfileImageUrl;
    /** 리액션 타입별 개수 */
    private List<MemberCardReactionCountDTO> reactionCounts;
    /** 현재 사용자가 남긴 리액션 타입 목록 */
    private List<CardReactionType> myReactions;
}
