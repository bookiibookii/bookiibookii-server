package com.example.bookiibookii.domain.notification.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationSuccessCode implements BaseCode{

    NOTIFICATION_LIST_OK(HttpStatus.OK, "NOTIFICATION200_1", "알림 목록 조회 성공"),
    NOTIFICATION_READ_OK(HttpStatus.OK, "NOTIFICATION200_2", "알림 읽음 처리 성공"),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
