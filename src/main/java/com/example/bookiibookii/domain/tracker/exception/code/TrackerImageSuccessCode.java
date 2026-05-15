package com.example.bookiibookii.domain.tracker.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TrackerImageSuccessCode implements BaseCode {
    TRACKING_PRESIGNED_URL_ISSUED(HttpStatus.OK, "TRACKIMG200_1", "배송 이미지 업로드용 Presigned URL을 발급했습니다."),

    RECEIVED_PRESIGNED_URL_ISSUED(HttpStatus.OK, "TRACKIMG200_2", "수령 이미지 업로드용 Presigned URL을 발급했습니다."),
    TRACKING_IMAGE_SAVED(HttpStatus.CREATED, "TRACKIMG201_1", "배송 이미지를 저장했습니다."),
    TRACKING_IMAGE_FOUND(HttpStatus.OK, "TRACKIMG200_3", "배송 이미지를 조회했습니다."),
    ;

    private final HttpStatus status;
    private final String code;
    private final String message;
}
