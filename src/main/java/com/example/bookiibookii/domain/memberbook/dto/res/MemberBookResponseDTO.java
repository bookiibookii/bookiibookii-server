package com.example.bookiibookii.domain.memberbook.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

public class MemberBookResponseDTO {

    @Getter
    @Builder
    @AllArgsConstructor
    public static class MypageBookDto {
        private String bookTitle;
        private Double rating;
    }
}
