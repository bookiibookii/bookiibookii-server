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
    BOOK_REVIEW_UPDATED(HttpStatus.OK, "REVIEW200_4", "책 리뷰가 수정되었습니다."),
    GROUP_REVIEWS_FOUND(HttpStatus.OK, "REVIEW200_5", "그룹 리뷰 조회에 성공했습니다."),
    MY_GROUP_REVIEWS_UPDATED(HttpStatus.OK, "REVIEW200_6", "내 리뷰가 수정되었습니다."),
    MY_BOOK_REVIEWS_FOUND(HttpStatus.OK, "REVIEW200_7", "내 책 리뷰 목록 조회에 성공했습니다."),
    MYPAGE_WRITTEN_REVIEWS_FOUND(HttpStatus.OK, "REVIEW200_8", "마이페이지 작성 후기 조회에 성공했습니다."),
    MYPAGE_RECEIVED_REVIEWS_FOUND(HttpStatus.OK, "REVIEW200_9", "마이페이지 받은 후기 조회에 성공했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
