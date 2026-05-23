package com.example.bookiibookii.domain.location.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum LocationSuccessCode implements BaseCode {
    GET_DELIVERIES_SUCCESS(HttpStatus.OK, "LOCATION200_1", "배송지 목록 조회에 성공했습니다."),
    ADD_DELIVERY_SUCCESS(HttpStatus.OK, "LOCATION200_2", "배송지 추가에 성공했습니다."),
    UPDATE_DELIVERY_SUCCESS(HttpStatus.OK, "LOCATION200_3", "배송지 수정에 성공했습니다."),
    DELETE_DELIVERY_SUCCESS(HttpStatus.OK, "LOCATION200_4", "배송지 삭제에 성공했습니다."),
    GET_EXCHANGES_SUCCESS(HttpStatus.OK, "LOCATION200_5", "희망 교환 장소 목록 조회에 성공했습니다."),
    ADD_EXCHANGE_SUCCESS(HttpStatus.OK, "LOCATION200_6", "희망 교환 장소 추가에 성공했습니다."),
    UPDATE_EXCHANGE_SUCCESS(HttpStatus.OK, "LOCATION200_7", "희망 교환 장소 수정에 성공했습니다."),
    DELETE_EXCHANGE_SUCCESS(HttpStatus.OK, "LOCATION200_8", "희망 교환 장소 삭제에 성공했습니다."),
    SET_DEFAULT_DELIVERY_SUCCESS(HttpStatus.OK, "LOCATION200_9", "대표 배송지 설정에 성공했습니다."),
    SET_DEFAULT_EXCHANGE_SUCCESS(HttpStatus.OK, "LOCATION200_10", "대표 희망 교환 장소 설정에 성공했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
