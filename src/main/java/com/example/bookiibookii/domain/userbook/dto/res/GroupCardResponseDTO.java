package com.example.bookiibookii.domain.userbook.dto.res;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 목록·상세 공통 카드 응답 DTO.
 * 목록: bookTitle 포함 (동일 UserBook이면 동일 책 제목).
 * 상세: bookTitle 포함.
 */
@Getter
@Builder
public class GroupCardResponseDTO {
    private Long cardId;
    private Integer page;
    private String memo;
    private CardImageResponseDTO cardImage;
    private LocalDateTime createdAt;
    /** 책 제목 (목록/상세 모두 설정) */
    private String bookTitle;
    /** 현재 로그인 사용자가 이 카드를 북마크했는지 (목록/상세/북마크 목록에서 사용) */
    private Boolean isBookmarked;
    /** 카드 작성자 이름 (그룹 목록에서 누가 썼는지 구분용) */
    private String creatorName;
}
