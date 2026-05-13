package com.example.bookiibookii.domain.groupbook.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CardErrorCode implements BaseCode {

    PAGE_EXCEEDS_TOTAL(HttpStatus.BAD_REQUEST, "CARD400_1", "입력하신 페이지가 도서의 전체 페이지를 초과합니다."),
    INVALID_PAGE_VALUE(HttpStatus.BAD_REQUEST, "CARD400_2", "페이지 번호는 0보다 커야 합니다."),
    CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "CARD404_1", "해당 독서 카드를 찾을 수 없습니다."),
    ALREADY_BOOKMARKED(HttpStatus.CONFLICT, "CARD409_1", "이미 북마크된 카드입니다."),
    /** 동시 요청 fallback: 유니크 위반 후 재조회까지 실패한 경우(데이터 접근/일관성 이슈) */
    CARD_STATE_CONFLICT(HttpStatus.CONFLICT, "CARD409_2", "일시적인 충돌이 발생했습니다. 다시 시도해주세요(동시성/데이터 접근 문제)."),
    BOOKMARKED_CARD_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "CARD400_3", "북마크된 카드입니다. 북마크를 취소하고 카드를 삭제해주세요.");


    private final HttpStatus status;
    private final String code;
    private final String message;

}
