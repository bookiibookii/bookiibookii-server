package com.example.bookiibookii.domain.userbook.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CardListResponseDTO {
    private Long groupId;       // 그룹 ID (그룹 멤버 간 공유 조회용)
    private String creatorName; // 해당 독서카드를 만든 사람(UserBook 소유자)의 이름
    private List<GroupCardResponseDTO> cards; // 각 카드에 bookTitle 포함
}
