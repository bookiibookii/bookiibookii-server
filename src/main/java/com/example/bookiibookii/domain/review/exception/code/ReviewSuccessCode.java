package com.example.bookiibookii.domain.review.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewSuccessCode implements BaseCode {
    BOOK_REVIEW_CREATED(HttpStatus.CREATED, "REVIEW201_2", "책 리뷰가 작성되었습니다."),
    MEMBER_REVIEW_CREATED(HttpStatus.CREATED, "REVIEW201_3", "파트너 후기가 작성되었습니다."),
    BOOK_REVIEW_UPDATED(HttpStatus.OK, "REVIEW200_4", "책 리뷰가 수정되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
