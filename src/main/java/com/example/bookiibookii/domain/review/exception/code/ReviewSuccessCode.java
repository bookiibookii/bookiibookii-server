package com.example.bookiibookii.domain.review.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewSuccessCode implements BaseCode {
    GROUP_REVIEW_CREATED(HttpStatus.CREATED, "REVIEW201_1", "그룹 리뷰가 작성되었습니다."),
    RELAY_REVIEW_HISTORY_FETCHED(HttpStatus.OK, "REVIEW200_3", "이어읽기 리뷰 히스토리를 성공적으로 조회했습니다."),
    BOOK_REVIEW_CREATED(HttpStatus.CREATED, "REVIEW201_2", "책 리뷰가 작성되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
