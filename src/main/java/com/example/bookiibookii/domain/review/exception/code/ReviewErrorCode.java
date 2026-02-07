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

    // 403
    NOT_GROUP_MEMBER(HttpStatus.FORBIDDEN, "REVIEW403_1", "해당 그룹의 멤버만 리뷰를 작성할 수 있습니다."),
    NOT_USER_BOOK_OWNER(HttpStatus.FORBIDDEN, "REVIEW403_2", "본인 서재(UserBook)가 아닙니다."),

    // 404
    USER_BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404_1", "UserBook을 찾을 수 없습니다."),
    MATCHED_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404_2", "그룹 매칭 정보를 찾을 수 없습니다."),
    PARTNER_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404_3", "리뷰 대상 파트너를 찾을 수 없습니다."),
    TRACKER_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404_4", "트래커를 찾을 수 없습니다."),
    TRACKER_NOT_RETURNED(HttpStatus.BAD_REQUEST, "REVIEW400_4", "호스트 회수완료 후에만 리뷰 작성이 가능합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
