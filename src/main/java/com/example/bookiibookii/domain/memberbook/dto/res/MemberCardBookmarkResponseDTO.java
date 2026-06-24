package com.example.bookiibookii.domain.memberbook.dto.res;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MemberCardBookmarkResponseDTO {
    /** 토글 후 북마크 여부 (true = 북마크됨, false = 해제됨) */
    private boolean bookmarked;
}
