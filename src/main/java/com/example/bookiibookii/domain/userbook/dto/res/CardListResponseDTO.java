package com.example.bookiibookii.domain.userbook.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CardListResponseDTO {
    private String title;  // userbook에 해당하는 책 제목
    private Long groupId;  // 그룹 ID (그룹 멤버 간 공유 조회용)
    private List<CardCreateResponseDTO> cards;
}
