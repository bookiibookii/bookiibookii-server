package com.example.bookiibookii.domain.group.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum MatchedMemberErrorCode implements BaseCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER404_1", "해당 그룹의 멤버가 아닙니다."),

    ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "MEMBER400_1", "이미 완독 처리가 완료된 도서입니다.");



    private final HttpStatus status;
    private final String code;
    private final String message;


}
