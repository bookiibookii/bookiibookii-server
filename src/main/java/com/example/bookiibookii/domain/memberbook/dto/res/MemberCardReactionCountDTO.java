package com.example.bookiibookii.domain.memberbook.dto.res;

import com.example.bookiibookii.domain.memberbook.enums.CardReactionType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberCardReactionCountDTO {
    private CardReactionType reaction;
    private long count;
}
