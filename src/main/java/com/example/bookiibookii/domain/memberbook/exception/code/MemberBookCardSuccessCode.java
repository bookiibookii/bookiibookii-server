package com.example.bookiibookii.domain.memberbook.exception.code;

import com.example.bookiibookii.global.apiPayload.code.BaseCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MemberBookCardSuccessCode implements BaseCode {

    PRESIGNED_URL_ISSUED(HttpStatus.OK, "MBCARD200_1", "카드 이미지 업로드용 Presigned URL을 발급했습니다."),
    CARD_CREATED(HttpStatus.CREATED, "MBCARD201_1", "독서카드를 생성했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
