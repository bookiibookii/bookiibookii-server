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
            "책은 최대 7개까지 설정 가능합니다.")
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
