package com.example.bookiibookii.domain.comment.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentSuccessCode implements BaseCode {

    CREATE_SUCCESS(HttpStatus.CREATED,
            "COMMENT201_1",
            "댓글을 성공적으로 생성했습니다."),
    CARD_CREATE_SUCCESS(HttpStatus.CREATED,
            "COMMENT201_2",
            "독서카드 댓글을 성공적으로 생성했습니다."),

    COMMENT_FOUND_OK(HttpStatus.OK,
            "COMMENT200_1",
            "댓글 조회에 성공했습니다."),
    CARD_COMMENT_FOUND_OK(HttpStatus.OK,
            "COMMENT200_2",
            "독서카드 댓글 조회에 성공했습니다."),

    DELETE_SUCCESS(HttpStatus.OK,
            "COMMENT200_3",
            "댓글 삭제에 성공했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
