package com.example.bookiibookii.domain.aladin.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AladinErrorCode implements BaseCode {
    // 404 Not Found
    ALADIN_NOT_FOUND(HttpStatus.NOT_FOUND, "ALADIN404_1", "알라딘 책을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
