package com.example.bookiibookii.domain.memberbook.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
public class MemberCardListResponseDTO {
    private Long groupId;
    private CurrentBookOwnerDto currentBookOwner;
    private List<MemberCardResponseDTO> cards;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentBookOwnerDto {
        private Long matchedMemberId;
        private String nickname;
    }
}
