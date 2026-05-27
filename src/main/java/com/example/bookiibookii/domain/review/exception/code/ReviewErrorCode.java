package com.example.bookiibookii.domain.review.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ReviewErrorCode implements BaseCode {

    // 400
    INVALID_RATING(HttpStatus.BAD_REQUEST, "REVIEW400_1", "평점은 0.0~5.0, 0.5 단위여야 합니다."),
    COMMENT_TOO_LONG(HttpStatus.BAD_REQUEST, "REVIEW400_2", "코멘트 길이 제한을 초과했습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "REVIEW400_3", "이미 리뷰를 작성했습니다."),
    COMMENT_REQUIRED(HttpStatus.BAD_REQUEST, "REVIEW400_4", "코멘트는 필수입니다."),
    INVALID_REVIEW_READING_STATUS(HttpStatus.BAD_REQUEST, "REVIEW400_5", "현재 독서 상태에서는 책 리뷰를 작성할 수 없습니다."),
    INVALID_MEMBER_REVIEW_STATUS(HttpStatus.BAD_REQUEST, "REVIEW400_6", "파트너 후기는 반납 교환이 모두 완료된 후 작성할 수 있습니다."),

    // 403
    NOT_GROUP_MEMBER(HttpStatus.FORBIDDEN, "REVIEW403_1", "해당 그룹의 멤버만 리뷰를 작성할 수 있습니다."),

    // 404
    MATCHED_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404_2", "그룹 매칭 정보를 찾을 수 없습니다."),
    PARTNER_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404_3", "상대 멤버를 찾을 수 없습니다."),
    CURRENT_MEMBER_BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404_5", "현재 읽고 있는 책을 찾을 수 없습니다."),
    BOOK_REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404_6", "책 리뷰를 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
