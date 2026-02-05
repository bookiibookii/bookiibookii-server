package com.example.bookiibookii.domain.tracker.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum TrackerImageErrorCode implements BaseCode {

    //400 BAD_REQUEST
    INVALID_S3_KEY_FORMAT(HttpStatus.BAD_REQUEST, "TRACKIMG400_1", "유효하지 않은 S3 키입니다. 형식: image/trackers/{uuid}"),
    DUPLICATE_S3_KEY(HttpStatus.BAD_REQUEST, "TRACKIMG400_2", "이미 사용 중인 S3 키입니다."),
    INVALID_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "TRACKIMG400_3", "유효하지 않은 이미지 타입입니다."),
    IMAGE_NOT_FOUND_IN_S3(HttpStatus.BAD_REQUEST, "TRACKIMG400_4", "S3에 해당 이미지가 존재하지 않습니다."),

    //500
    S3_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "TRACKIMG500_1", "S3 접근 중 오류가 발생했습니다."),

    //404 NOT_FOUND
    TRACKING_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKIMG404_1", "해당 배송 이미지가 존재하지 않습니다."),
    RECEIVED_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "TRACKIMG404_2", "해당 수령 이미지가 존재하지 않습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
