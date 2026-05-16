package com.example.bookiibookii.domain.memberbook.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MemberCardListResponseDTO {
    private Long groupId;
    private List<MemberCardResponseDTO> cards;
}
