package com.example.bookiibookii.domain.notification.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum NotificationErrorCode implements BaseCode{

    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION404_1", "알림을 찾을 수 없습니다."),
    NOTIFICATION_FORBIDDEN(HttpStatus.FORBIDDEN, "NOTIFICATION403_1", "해당 알림에 접근할 수 없습니다."),
    INVALID_CURSOR(HttpStatus.BAD_REQUEST, "NOTIFICATION400_1", "커서 형식이 올바르지 않습니다."),
    NOTIFICATION_SERIALIZE_PAYLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,
            "NOTIFICATION500_01",
            "알림 페이로드 직렬화에 실패했습니다."
    );

    private final HttpStatus status;
    private final String code;
    private final String message;
}

