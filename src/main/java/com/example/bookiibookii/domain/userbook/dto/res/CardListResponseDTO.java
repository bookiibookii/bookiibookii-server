package com.example.bookiibookii.domain.userbook.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CardListResponseDTO {
    private String title;  // userbook에 해당하는 책 제목
    private List<CardCreateResponseDTO> cards;
}
