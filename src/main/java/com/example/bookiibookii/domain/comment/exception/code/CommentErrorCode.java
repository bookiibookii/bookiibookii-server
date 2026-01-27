package com.example.bookiibookii.domain.comment.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommentErrorCode implements BaseCode {

    PARENT_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND,
            "COMMENT404_1",
            "부모 댓글을 찾을 수 없습니다."),
    PARENT_COMMENT_GROUP_MISMATCH(HttpStatus.BAD_REQUEST,
            "COMMENT400_1",
            "부모 댓글이 해당 그룹에 속하지 않습니다."),
    REPLY_DEPTH_EXCEEDED(HttpStatus.BAD_REQUEST,
            "COMMENT400_2",
            "대댓글의 대댓글은 허용되지 않습니다."),
    COMMENT_WRITE_FORBIDDEN(HttpStatus.FORBIDDEN,
            "COMMENT403_1",
            "진행 중인 그룹에는 그룹 멤버만 댓글 작성이 가능합니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
