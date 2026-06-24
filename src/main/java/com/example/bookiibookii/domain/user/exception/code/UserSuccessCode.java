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
    FAVORITE_BOOK_ADD_SUCCESS(HttpStatus.OK,
            "USER200_6",
            "인생 책 등록에 성공했습니다."),
    FAVORITE_BOOK_DELETE_SUCCESS(HttpStatus.OK,
            "USER200_7",
            "인생 책 삭제에 성공했습니다."),
    REPRESENTATIVE_BOOK_ADD_SUCCESS(HttpStatus.OK,
            "USER200_8",
            "대표 책 등록에 성공했습니다."),
    REPRESENTATIVE_BOOK_DELETE_SUCCESS(HttpStatus.OK,
            "USER200_9",
            "대표 책 삭제에 성공했습니다."),
    REPRESENTATIVE_BOOK_REORDER_SUCCESS(HttpStatus.OK,
            "USER200_10",
            "대표 책 순서 변경에 성공했습니다."),
    UPDATE_INTRODUCTION_SUCCESS(HttpStatus.OK,
            "USER200_11",
            "한줄 소개 수정에 성공했습니다."),
    WITHDRAWAL_SUCCESS(HttpStatus.OK,
            "USER200_12",
            "회원 탈퇴가 완료되었습니다."),
    FAVORITE_BOOK_REPLACE_SUCCESS(HttpStatus.OK,
            "USER200_14",
            "인생 책 교체에 성공했습니다."),
    PROFILE_SHARE_TOKEN_CREATED(HttpStatus.CREATED,
            "USER201_1",
            "프로필 공유 링크를 발급했습니다."),
    PUBLIC_PROFILE_FOUND(HttpStatus.OK,
            "USER200_13",
            "공유 프로필을 조회했습니다."),
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
