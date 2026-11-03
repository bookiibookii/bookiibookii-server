package com.example.bookiibookii.domain.nlgoKr.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SeojiErrorCode implements BaseCode {

    SEOJI_NOT_FOUND(HttpStatus.NOT_FOUND, "SEOJI404_1", "국립중앙도서관에서 책을 찾을 수 없습니다."),
    SEOJI_RESPONSE_PARSE_ERROR(HttpStatus.BAD_GATEWAY, "SEOJI502_1", "국립중앙도서관 응답을 처리할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
