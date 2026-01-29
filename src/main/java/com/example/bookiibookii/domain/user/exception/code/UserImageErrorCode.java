package com.example.bookiibookii.domain.user.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserImageErrorCode implements BaseCode {

    //400 BAD_REQUEST
    INVALID_S3_KEY_FORMAT(HttpStatus.BAD_REQUEST, "USERIMG400_1", "유효하지 않은 S3 키입니다. 형식: image/users/{userId}/{uuid}"),
    DUPLICATE_S3_KEY(HttpStatus.BAD_REQUEST, "USERIMG400_2", "이미 사용 중인 S3 키입니다."),
    IMAGE_NOT_FOUND_IN_S3(HttpStatus.BAD_REQUEST, "USERIMG400_3", "S3에 해당 이미지가 존재하지 않습니다."),
    S3_ACCESS_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "USERIMG500_1", "S3 접근 중 오류가 발생했습니다."),

    //404 NOT_FOUND
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USERIMG404_1", "해당 사용자를 찾을 수 없습니다."),
    USER_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "USERIMG404_2", "해당 사용자 이미지가 존재하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
