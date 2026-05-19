package com.example.bookiibookii.domain.memberbook.dto.req;

import lombok.Builder;
import lombok.Getter;

public class LibraryMemberBookRequestDTO {

    @Builder
    @Getter
    public static class SearchDTO {
        /** 검색어 (그룹명, 도서명, 저자명) */
        private String keyword;
    }
}
