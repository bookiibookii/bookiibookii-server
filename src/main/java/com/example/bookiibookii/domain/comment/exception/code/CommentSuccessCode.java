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
    COMMENT_FOUND_OK(HttpStatus.OK,
            "COMMENT200_1",
            "그룹의 댓글을 성공적으로 찾았습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
