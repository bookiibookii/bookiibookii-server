package com.example.bookiibookii.domain.user.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum UserImageSuccessCode implements BaseCode {
    PRESIGNED_URL_ISSUED(HttpStatus.OK, "USERIMG200_1", "사용자 이미지 업로드용 Presigned URL을 발급했습니다."),
    USER_IMAGE_SAVED(HttpStatus.CREATED, "USERIMG201_1", "사용자 이미지를 저장했습니다."),
    USER_IMAGE_UPDATED(HttpStatus.OK, "USERIMG200_3", "사용자 이미지를 업데이트했습니다."),
    USER_IMAGE_FOUND(HttpStatus.OK, "USERIMG200_2", "사용자 이미지를 조회했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
