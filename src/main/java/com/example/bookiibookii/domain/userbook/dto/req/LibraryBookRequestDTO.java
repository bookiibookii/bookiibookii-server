package com.example.bookiibookii.domain.userbook.dto.req;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


public class LibraryBookRequestDTO {

    @Builder
    @Getter
    public static class SearchDTO {
        private String keyword; // 검색어 (제목, 저자, 한줄평)
    }
}
