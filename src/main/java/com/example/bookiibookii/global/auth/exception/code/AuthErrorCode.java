package com.example.bookiibookii.global.auth.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements BaseCode {

    NOT_FOUND_REFRESH_TOKEN(HttpStatus.NOT_FOUND,
            "AUTH404_1",
            "RefreshToken을 찾을 수 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND,
            "AUTH404_2",
            "토큰에 저장된 해당 사용자를 찾을 수 없습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.BAD_REQUEST,
            "AUTH400_1",
            "RefreshToken이 불일치합니다."),
    UNSUPPORTED_SOCIAL_TYPE(HttpStatus.BAD_REQUEST,
            "AUTH400_2",
            "지원하지 않는 소셜 로그인 방식입니다."),
    INVALID_SOCIAL_TOKEN(HttpStatus.UNAUTHORIZED,
            "AUTH401_1",
            "소셜 인증에 실패했습니다."),
    ;
    private final HttpStatus status;
    private final String code;
    private final String message;
}
