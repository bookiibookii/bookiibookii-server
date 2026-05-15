package com.example.bookiibookii.domain.user.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserErrorCode implements BaseCode {
    NOT_FOUND(HttpStatus.NOT_FOUND,
            "USER404_1",
            "사용자를 찾을 수 없습니다."),
    USER_TAG_NOT_FOUND(HttpStatus.NOT_FOUND,
            "USERTAG404_1",
            "사용자의 태그가 존재하지 않습니다."),
    USER_WITHDRAWN(HttpStatus.FORBIDDEN,
            "USER403_1",
            "탈퇴한 사용자입니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST,
            "USER400_1",
            "사용 불가능한 닉네임입니다.")
    ,
    NICKNAME_DUPLICATE(HttpStatus.BAD_REQUEST,
            "USER400_2",
            "이미 사용 중인 닉네임입니다."),
    NICKNAME_BAD_WORD(HttpStatus.BAD_REQUEST,
            "USER400_3",
            "닉네임에 금칙어가 포함되어 있습니다."),
    SOCIAL_USER_CREATE_RACE_CONDITION(HttpStatus.INTERNAL_SERVER_ERROR,
            "USER500_1",
            "소셜 사용자 생성 중 일시적인 충돌이 발생했습니다. 다시 시도해주세요."),
    USER_PICK_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST,
            "USER400_4",
            "책은 최대 7개까지 설정 가능합니다."),
    FAVORITE_BOOK_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST,
            "USER400_5",
            "인생 책은 최대 3개까지 등록 가능합니다."),
    FAVORITE_BOOK_ALREADY_EXISTS(HttpStatus.BAD_REQUEST,
            "USER400_6",
            "이미 인생 책으로 등록된 책입니다."),
    USER_BOOK_NOT_FOUND(HttpStatus.NOT_FOUND,
            "USER404_2",
            "등록된 책을 찾을 수 없습니다."),
    NOT_ELIGIBLE_FOR_REPRESENTATIVE(HttpStatus.BAD_REQUEST,
            "USER400_7",
            "대표책으로 등록할 수 없는 책입니다. 인생책이거나 별점을 등록한 완독책만 가능합니다."),
    INVALID_REPRESENTATIVE_ORDER(HttpStatus.BAD_REQUEST,
            "USER400_8",
            "대표책 순서가 유효하지 않습니다. 현재 대표책 전체를 중복 없이 포함해야 합니다.")
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
