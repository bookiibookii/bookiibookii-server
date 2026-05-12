package com.example.bookiibookii.domain.groupbook.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class GroupBookResponseDTO {
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MypageBookDto {
        private String bookTitle;
        private Double rating;
    }
}
