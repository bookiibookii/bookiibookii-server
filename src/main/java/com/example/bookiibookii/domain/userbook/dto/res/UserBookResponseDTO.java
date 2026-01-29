package com.example.bookiibookii.domain.userbook.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class UserBookResponseDTO {
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MypageBookDto {
        private String bookTitle;
        private Double rating;
    }
}
