package com.example.bookiibookii.domain.memberbook.dto.res;

import com.example.bookiibookii.domain.memberbook.enums.CardReactionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberCardReactionToggleResponseDTO {
    private CardReactionType reaction;
    /** 토글 후 해당 리액션 적용 여부 (true = 적용됨, false = 취소됨) */
    private boolean active;
}
