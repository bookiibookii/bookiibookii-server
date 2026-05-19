package com.example.bookiibookii.domain.memberbook.dto.req;

import com.example.bookiibookii.domain.memberbook.enums.CardReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class MemberCardReactionToggleRequestDTO {

    @NotNull(message = "리액션 타입은 필수입니다.")
    private CardReactionType reaction;
}
