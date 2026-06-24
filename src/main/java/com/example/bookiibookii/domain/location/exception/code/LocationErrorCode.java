package com.example.bookiibookii.domain.location.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LocationErrorCode implements BaseCode {
    NOT_FOUND(HttpStatus.NOT_FOUND, "LOCATION404_1", "장소를 찾을 수 없습니다."),
    LOCATION_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "LOCATION400_1", "최대 2개까지 등록 가능합니다."),
    EXCHANGE_COORDINATES_REQUIRED(HttpStatus.BAD_REQUEST, "LOCATION400_2", "희망 교환 장소는 좌표가 필요합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
