package com.example.bookiibookii.domain.user.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserSuccessCode implements BaseCode {
    OK(HttpStatus.OK,
            "USER200_1",
            "사용자를 찾았습니다."),
    ONBOARDING_SUCCESS(HttpStatus.OK,
            "USER200_2",
            "온보딩 설정이 완료되었습니다."),
    GET_MYPAGE_SUCCESS(HttpStatus.OK,
            "USER200_3",
            "마이페이지 조회에 성공했습니다."),
    UPDATE_MYPAGE_SUCCESS(HttpStatus.OK,
            "USER200_4",
            "마이페이지 수정에 성공했습니다."),
    GET_BOOKSHELF_SUCCESS(HttpStatus.OK,
            "USER200_5",
            "나의 책장 조회에 성공했습니다."),
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
