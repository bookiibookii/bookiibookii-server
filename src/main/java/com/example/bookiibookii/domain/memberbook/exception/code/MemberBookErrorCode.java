package com.example.bookiibookii.domain.memberbook.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberBookErrorCode implements BaseCode {

    MEMBER_BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "MB404_1", "해당 멤버북을 찾을 수 없습니다."),
    MATCHED_MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MB404_2", "해당 그룹의 매칭 멤버를 찾을 수 없습니다."),
    MEMBER_BOOK_REMOVED(HttpStatus.BAD_REQUEST, "MB400_1", "서재에서 제거된 멤버북에는 독서카드를 생성할 수 없습니다."),
    QUOTATION_REQUIRED(HttpStatus.BAD_REQUEST, "MB400_2", "TEXT 타입 독서카드는 quotation이 필수입니다."),
    S3_KEY_REQUIRED(HttpStatus.BAD_REQUEST, "MB400_3", "IMAGE 타입 독서카드는 s3Key가 필수입니다."),
    INVALID_PAGE_VALUE(HttpStatus.BAD_REQUEST, "MB400_4", "페이지 번호는 0보다 커야 합니다."),
    PAGE_EXCEEDS_TOTAL(HttpStatus.BAD_REQUEST, "MB400_5", "입력하신 페이지가 도서의 전체 페이지를 초과합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
